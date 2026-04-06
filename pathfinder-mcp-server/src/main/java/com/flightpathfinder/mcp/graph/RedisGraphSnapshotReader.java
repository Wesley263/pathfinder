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
 * 从 Redis 读取图快照读模型。
 *
 * 实现按照图键先解析最新版本，再加载快照载荷并校验 schema 兼容性。
 */
@Component
public class RedisGraphSnapshotReader implements GraphSnapshotReader {

    /** 访问图快照键值的 Redis 客户端。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 用于反序列化图快照 JSON。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造 Redis 图快照读取器。
     *
     * @param stringRedisTemplate Redis 操作模板
     * @param objectMapper JSON 对象映射器
     */
    public RedisGraphSnapshotReader(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 按图键加载当前最新版本的快照。
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
        * @param payload Redis 中存储的快照 JSON
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
