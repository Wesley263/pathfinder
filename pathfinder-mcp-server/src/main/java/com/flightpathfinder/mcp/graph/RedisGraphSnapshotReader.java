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
 * MCP server 侧基于 Redis 的图快照读取器。
 *
 * <p>读取器保持严格只读。MCP server 不应在 snapshot miss 时重建图或回查
 * bootstrap 数据表，只消费已发布读模型。</p>
 */
@Component
public class RedisGraphSnapshotReader implements GraphSnapshotReader {

    /** Redis 访问模板。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** JSON 反序列化映射器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造 Redis 图快照读取器。
     *
     * @param stringRedisTemplate Redis 模板
     * @param objectMapper JSON 映射器
     */
    public RedisGraphSnapshotReader(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 从 Redis 读取指定 graphKey 的最新已发布快照。
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
     * @param payload JSON 载荷
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
