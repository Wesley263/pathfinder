package com.flightpathfinder.admin.service.etl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.admin.config.AdminDataEtlProperties;
import com.flightpathfinder.admin.service.AdminDatasetReloadResult;
import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 城市成本数据导入器。
 *
 * 从配置资源读取城市生活成本数据并增量写入 t_city_cost 表。
 */
@Service
public class CityCostDatasetImporter implements AdminDatasetImporter {

    private static final String UPSERT_SQL = """
            INSERT INTO t_city_cost (
                iata_code,
                city,
                country,
                country_code,
                daily_cost_usd,
                accommodation_usd,
                meal_cost_usd,
                transportation_usd,
                created_at,
                updated_at,
                deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            ON CONFLICT (iata_code)
            DO UPDATE SET
                city = EXCLUDED.city,
                country = EXCLUDED.country,
                country_code = EXCLUDED.country_code,
                daily_cost_usd = EXCLUDED.daily_cost_usd,
                accommodation_usd = EXCLUDED.accommodation_usd,
                meal_cost_usd = EXCLUDED.meal_cost_usd,
                transportation_usd = EXCLUDED.transportation_usd,
                updated_at = CURRENT_TIMESTAMP,
                deleted = 0
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final AdminDataEtlProperties properties;
    private final ObjectMapper objectMapper;

    public CityCostDatasetImporter(JdbcTemplate jdbcTemplate,
                                   ResourceLoader resourceLoader,
                                   AdminDataEtlProperties properties,
                                   ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
          * 返回当前导入器负责的数据集标识。
     *
      * @return 固定数据集标识 city_cost
     */
    @Override
    public String datasetId() {
        return "city_cost";
    }

    /**
          * 执行城市成本数据重载。
     *
      * @return 本次重载的处理统计与状态
     */
    @Override
    public AdminDatasetReloadResult reload() {
        Instant startedAt = Instant.now();
        String sourceLocation = properties.getCityCost().getLocation();
        Resource resource = resourceLoader.getResource(sourceLocation);
        if (!resource.exists()) {
            return new AdminDatasetReloadResult(
                    datasetId(),
                    "MISSING",
                    "city cost resource does not exist",
                    List.of(sourceLocation),
                    0L,
                    0L,
                    0L,
                    startedAt,
                    Instant.now(),
                    Map.of("missingLocation", sourceLocation));
        }

        long processed = 0L;
        long upserted = 0L;
        long failed = 0L;
        try (InputStream inputStream = resource.getInputStream()) {
            List<Map<String, Object>> items = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            for (Map<String, Object> item : items) {
                processed++;
                try {
                    String iataCode = normalizeCode(stringValue(item.get("iata_code")));
                    if (iataCode.isBlank()) {
                        failed++;
                        continue;
                    }
                    jdbcTemplate.update(
                            UPSERT_SQL,
                            iataCode,
                            trimToNull(stringValue(item.get("city"))),
                            trimToNull(stringValue(item.get("country"))),
                            normalizeCode(stringValue(item.get("country_code"))),
                            doubleValue(item.get("daily_cost_usd")),
                            doubleValue(item.get("accommodation_usd")),
                            doubleValue(item.get("meal_cost_usd")),
                            doubleValue(item.get("transportation_usd")));
                    upserted++;
                } catch (RuntimeException exception) {
                    failed++;
                }
            }
            // 以数据集粒度保留结果，便于识别部分导入成功而非掩盖行级缺口。
            String status = failed > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";
            String reason = failed > 0
                    ? "city cost ETL completed with row-level failures"
                    : "city cost ETL completed";
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("resourceType", resourceDescription(resource));
            details.put("rowsRead", processed);
            details.put("rowsUpserted", upserted);
            details.put("rowsFailed", failed);
            return new AdminDatasetReloadResult(
                    datasetId(),
                    status,
                    reason,
                    List.of(sourceLocation),
                    processed,
                    upserted,
                    failed,
                    startedAt,
                    Instant.now(),
                    Map.copyOf(details));
        } catch (Exception exception) {
            return new AdminDatasetReloadResult(
                    datasetId(),
                    "FAILED",
                    "city cost ETL failed: " + exception.getMessage(),
                    List.of(sourceLocation),
                    processed,
                    upserted,
                    Math.max(failed, 1L),
                    startedAt,
                    Instant.now(),
                    Map.of("resourceType", resourceDescription(resource)));
        }
    }

    private String resourceDescription(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (Exception exception) {
            return resource.getDescription();
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception exception) {
            return 0.0D;
        }
    }
}
