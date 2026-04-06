package com.flightpathfinder.admin.service.impl;

import com.flightpathfinder.admin.service.AdminCacheInvalidateResult;
import com.flightpathfinder.admin.service.AdminDatasetReloadResult;
import com.flightpathfinder.admin.service.AdminDataReloadResult;
import com.flightpathfinder.admin.service.AdminDataService;
import com.flightpathfinder.admin.service.AdminDataStats;
import com.flightpathfinder.admin.service.etl.AdminDatasetImporter;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotLifecycleService;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.exception.AbstractException;
import com.flightpathfinder.framework.exception.ServiceException;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 管理端 ETL 重载、数据统计与缓存失效的默认编排实现。
 *
 * <p>该服务面向管理端运维任务，负责导入数据集及其下游缓存治理。
 * 其中 {@code data/reload} 不承担图快照重建职责，快照重建保持为独立图管理动作。
 */
@Service
public class DefaultAdminDataService implements AdminDataService {

    private static final String AIRPORT_COUNT_SQL = """
            SELECT COUNT(1)
            FROM t_airport
            WHERE deleted = 0
              AND iata_code IS NOT NULL
              AND iata_code <> ''
            """;

    private static final String AIRLINE_COUNT_SQL = """
            SELECT COUNT(1)
            FROM t_airline
            WHERE deleted = 0
            """;

    private static final String ROUTE_COUNT_SQL = """
            SELECT COUNT(1)
            FROM t_route
            WHERE deleted = 0
              AND source_airport_iata IS NOT NULL
              AND source_airport_iata <> ''
              AND dest_airport_iata IS NOT NULL
              AND dest_airport_iata <> ''
            """;

    private static final String VISA_POLICY_COUNT_SQL = """
            SELECT COUNT(1)
            FROM t_visa_policy
            WHERE deleted = 0
            """;

    private static final String CITY_COST_COUNT_SQL = """
            SELECT COUNT(1)
            FROM t_city_cost
            WHERE deleted = 0
            """;

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final GraphSnapshotLifecycleService graphSnapshotLifecycleService;
    private final GraphSnapshotProperties graphSnapshotProperties;
    private final LocalMcpToolRegistry localMcpToolRegistry;
    private final List<AdminDatasetImporter> datasetImporters;

    public DefaultAdminDataService(JdbcTemplate jdbcTemplate,
                                   StringRedisTemplate stringRedisTemplate,
                                   GraphSnapshotLifecycleService graphSnapshotLifecycleService,
                                   GraphSnapshotProperties graphSnapshotProperties,
                                   LocalMcpToolRegistry localMcpToolRegistry,
                                   List<AdminDatasetImporter> datasetImporters) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.graphSnapshotLifecycleService = graphSnapshotLifecycleService;
        this.graphSnapshotProperties = graphSnapshotProperties;
        this.localMcpToolRegistry = localMcpToolRegistry;
        this.datasetImporters = List.copyOf(datasetImporters);
    }

    /**
        * 执行管理端触发的外部数据集 ETL，并在数据变化时失效依赖读面。
     *
     * @param graphKey graph key whose published snapshot should be invalidated if data changes
     * @param reason operator-supplied reason for the reload
     * @return overall reload result including dataset-level outcomes and refreshed stats
     */
    @Override
    public AdminDataReloadResult reload(String graphKey, String reason) {
        String resolvedGraphKey = resolveGraphKey(graphKey);
        String resolvedReason = normalizeReason(reason, "manual admin data reload");
        try {
            List<AdminDatasetReloadResult> datasetResults = datasetImporters.stream()
                    .sorted(Comparator.comparingInt(this::datasetOrder))
                    .map(this::safeReload)
                    .toList();
            boolean hasMaterialChange = datasetResults.stream().anyMatch(result -> result.upsertedCount() > 0L);
                    // 该接口严格限定为“外部数据导入”，仅失效依赖面，由运维自行决定何时在图管理侧重建快照。
            List<String> invalidatedScopes = hasMaterialChange
                    ? invalidateDataSurfaces(resolvedGraphKey)
                    : List.of();
            return new AdminDataReloadResult(
                    resolvedGraphKey,
                    determineOverallStatus(datasetResults),
                    resolvedReason,
                    Instant.now(),
                    datasetResults,
                    currentStats(),
                    invalidatedScopes,
                    hasMaterialChange);
        } catch (AbstractException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException(
                    BaseErrorCode.SERVICE_ERROR,
                    "failed to reload external admin datasets for graphKey=" + resolvedGraphKey + ": " + exception.getMessage());
        }
    }

    /**
        * 返回管理端数据集当前聚合计数。
     *
     * @return current table counts used by the admin stats view
     */
    @Override
    public AdminDataStats currentStats() {
        try {
            return new AdminDataStats(
                    queryCount(AIRPORT_COUNT_SQL),
                    queryCount(AIRLINE_COUNT_SQL),
                    queryCount(ROUTE_COUNT_SQL),
                    queryCount(VISA_POLICY_COUNT_SQL),
                    queryCount(CITY_COST_COUNT_SQL),
                    Instant.now());
        } catch (AbstractException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR, "failed to query current data stats: " + exception.getMessage());
        }
    }

    /**
        * 失效依赖导入数据的缓存面。
     *
     * @param graphKey graph key whose published snapshot should be invalidated
     * @param reason operator-supplied reason for the invalidation
     * @return cache invalidation summary
     */
    @Override
    public AdminCacheInvalidateResult invalidateCaches(String graphKey, String reason) {
        String resolvedGraphKey = resolveGraphKey(graphKey);
        String resolvedReason = normalizeReason(reason, "manual admin cache invalidate");
        try {
            long clearedGraphSnapshotKeyCount = countSnapshotKeys(resolvedGraphKey);
            graphSnapshotLifecycleService.invalidate(resolvedGraphKey);
            int clearedMcpToolCount = clearLocalMcpToolRegistry();
            return new AdminCacheInvalidateResult(
                    resolvedGraphKey,
                    "INVALIDATED",
                    resolvedReason,
                    Instant.now(),
                    clearedGraphSnapshotKeyCount,
                    clearedMcpToolCount,
                    List.of("graph-snapshot", "mcp-tool-registry"));
        } catch (AbstractException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException(
                    BaseErrorCode.SERVICE_ERROR,
                    "failed to invalidate admin caches for graphKey=" + resolvedGraphKey + ": " + exception.getMessage());
        }
    }

    private long queryCount(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count;
    }

    private long countSnapshotKeys(String graphKey) {
        Set<String> keys = stringRedisTemplate.keys(GraphSnapshotRedisKeys.snapshotKeyPattern(graphKey));
        return keys == null ? 0L : keys.size();
    }

    private List<String> invalidateDataSurfaces(String graphKey) {
        // 失效与重建解耦，便于先完成数据重载，再由运维在可控时机发布新图快照。
        graphSnapshotLifecycleService.invalidate(graphKey);
        clearLocalMcpToolRegistry();
        return List.of("graph-snapshot", "mcp-tool-registry");
    }

    private AdminDatasetReloadResult safeReload(AdminDatasetImporter datasetImporter) {
        try {
            return datasetImporter.reload();
        } catch (RuntimeException exception) {
            return new AdminDatasetReloadResult(
                    datasetImporter.datasetId(),
                    "FAILED",
                    "dataset reload failed: " + exception.getMessage(),
                    List.of(),
                    0L,
                    0L,
                    1L,
                    Instant.now(),
                    Instant.now(),
                    java.util.Map.of());
        }
    }

    private int clearLocalMcpToolRegistry() {
        int currentToolCount = localMcpToolRegistry.listTools().size();
        localMcpToolRegistry.replaceAll(List.of());
        return currentToolCount;
    }

    private String determineOverallStatus(List<AdminDatasetReloadResult> datasetResults) {
        // 保留数据集粒度结果，避免把部分成功压缩为不可解释的单一成败标记。
        if (datasetResults.isEmpty()) {
            return "FAILED";
        }
        long successCount = datasetResults.stream().filter(result -> "SUCCESS".equals(result.status())).count();
        long partialCount = datasetResults.stream().filter(result -> "PARTIAL_SUCCESS".equals(result.status())).count();
        long missingCount = datasetResults.stream().filter(result -> "MISSING".equals(result.status())).count();
        if (successCount == datasetResults.size()) {
            return "SUCCESS";
        }
        if (successCount == 0L && partialCount == 0L && missingCount == datasetResults.size()) {
            return "MISSING";
        }
        if (successCount == 0L && partialCount == 0L) {
            return "FAILED";
        }
        return "PARTIAL_SUCCESS";
    }

    private int datasetOrder(AdminDatasetImporter datasetImporter) {
        return switch (datasetImporter.datasetId()) {
            case "openflights" -> 10;
            case "visa" -> 20;
            case "city_cost" -> 30;
            default -> 100;
        };
    }

    private String resolveGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey.trim();
    }

    private String normalizeReason(String reason, String defaultValue) {
        return reason == null || reason.isBlank()
                ? defaultValue
                : reason.trim();
    }
}
