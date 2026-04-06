package com.flightpathfinder.flight.core.graph.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.exception.ServiceException;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotVersions;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 主应用侧已发布图快照只读查询服务。
 *
 * 说明。
 * 说明。
 */
@Service
public class RedisGraphSnapshotQueryService implements GraphSnapshotQueryService {

    /** 注释说明。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 快照反序列化对象映射器。 */
    private final ObjectMapper objectMapper;
    /** 图快照配置。 */
    private final GraphSnapshotProperties graphSnapshotProperties;

    /**
     * 说明。
     *
     * @param stringRedisTemplate 参数说明。
     * @param objectMapper 参数说明。
     * @param graphSnapshotProperties 图快照配置
     */
    public RedisGraphSnapshotQueryService(StringRedisTemplate stringRedisTemplate,
                                          ObjectMapper objectMapper,
                                          GraphSnapshotProperties graphSnapshotProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    /**
        * 说明。
     *
        * @param graphKey 图逻辑标识
        * @return 当前快照（存在时返回）
     */
    @Override
    public Optional<GraphSnapshot> loadCurrent(String graphKey) {
        String normalizedGraphKey = normalizeGraphKey(graphKey);
        String latestVersion = stringRedisTemplate.opsForValue().get(GraphSnapshotRedisKeys.latestVersionKey(normalizedGraphKey));
        if (latestVersion == null || latestVersion.isBlank()) {
            return Optional.empty();
        }

        String payload = stringRedisTemplate.opsForValue().get(GraphSnapshotRedisKeys.snapshotKey(normalizedGraphKey, latestVersion));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }

        GraphSnapshot snapshot = deserialize(payload);
        if (!GraphSnapshotVersions.supports(snapshot.schemaVersion())) {
            throw new ServiceException(
                    BaseErrorCode.SERVICE_ERROR,
                    "unsupported graph snapshot schema version: " + snapshot.schemaVersion());
        }
        // 说明。
        return Optional.of(snapshot);
    }

    /**
     * 反序列化快照载荷。
     *
     * @param payload 参数说明。
     * @return 图快照对象
     */
    private GraphSnapshot deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, GraphSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR, "failed to deserialize current graph snapshot payload");
        }
    }

    /**
     * 说明。
     *
     * @param graphKey 原始图标识
     * @return 归一化后的图标识
     */
    private String normalizeGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey.trim();
    }
}

