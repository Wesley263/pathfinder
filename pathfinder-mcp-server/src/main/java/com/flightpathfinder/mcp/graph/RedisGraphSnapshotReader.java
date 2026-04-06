package com.flightpathfinder.mcp.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotVersions;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
@Component
public class RedisGraphSnapshotReader implements GraphSnapshotReader {

    /** 注释说明。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 注释说明。 */
    private final ObjectMapper objectMapper;

    /**
     * 说明。
     *
     * @param stringRedisTemplate 参数说明。
     * @param objectMapper 参数说明。
     */
    public RedisGraphSnapshotReader(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 说明。
     *
     * @param graphKey 图逻辑标识
     * @return 最新快照（存在时返回）
     */
    @Override
    public Optional<GraphSnapshot> loadLatest(String graphKey) {
        String latestVersion = stringRedisTemplate.opsForValue().get(GraphSnapshotRedisKeys.latestVersionKey(graphKey));
        if (latestVersion == null || latestVersion.isBlank()) {
            return Optional.empty();
        }

        String payload = stringRedisTemplate.opsForValue().get(GraphSnapshotRedisKeys.snapshotKey(graphKey, latestVersion));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }

        GraphSnapshot snapshot = deserialize(payload);
        if (!GraphSnapshotVersions.supports(snapshot.schemaVersion())) {
            throw new IllegalStateException("Unsupported graph snapshot schema version: " + snapshot.schemaVersion());
        }
        return Optional.of(snapshot);
    }

    /**
     * 反序列化图快照载荷。
     *
     * @param payload 参数说明。
     * @return 图快照对象
     */
    private GraphSnapshot deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, GraphSnapshot.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize graph snapshot payload", ex);
        }
    }
}
