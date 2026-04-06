package com.flightpathfinder.flight.core.graph.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 图快照读模型的 Redis 发布实现。
 *
 * <p>发布与构建分离，是因为图快照是跨进程共享读模型，
 * 特别由 MCP server 消费，而非仅 bootstrap 进程内使用。</p>
 */
@Component
public class RedisGraphSnapshotPublisher implements GraphSnapshotPublisher {

    /** 用于访问 Redis 的模板。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 快照序列化对象映射器。 */
    private final ObjectMapper objectMapper;
    /** 图快照配置。 */
    private final GraphSnapshotProperties graphSnapshotProperties;

    /**
     * 构造 Redis 图快照发布器。
     *
     * @param stringRedisTemplate Redis 模板
     * @param objectMapper JSON 映射器
     * @param graphSnapshotProperties 图快照配置
     */
    public RedisGraphSnapshotPublisher(StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       GraphSnapshotProperties graphSnapshotProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    /**
        * 把快照载荷和最新版本指针发布到 Redis。
     *
        * @param graphKey 图逻辑标识
        * @param snapshot 待发布快照
     */
    @Override
    public void publish(String graphKey, GraphSnapshot snapshot) {
        String normalizedGraphKey = normalizeGraphKey(graphKey);
        String latestKey = GraphSnapshotRedisKeys.latestVersionKey(normalizedGraphKey);
        String snapshotKey = GraphSnapshotRedisKeys.snapshotKey(normalizedGraphKey, snapshot.snapshotVersion());
        String payload = toJson(snapshot);

        // 快照内容键与 latest 指针都做版本化，读侧可保持严格只读且无需与构建进程协同。
        stringRedisTemplate.opsForValue().set(snapshotKey, payload, graphSnapshotProperties.getTtl());
        stringRedisTemplate.opsForValue().set(latestKey, snapshot.snapshotVersion(), graphSnapshotProperties.getTtl());
    }

    /**
     * 归一化 graphKey。
     *
     * @param graphKey 原始图标识
     * @return 归一化后的图标识
     */
    private String normalizeGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey;
    }

    /**
     * 序列化快照对象。
     *
     * @param snapshot 图快照
     * @return JSON 字符串
     */
    private String toJson(GraphSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize graph snapshot", ex);
        }
    }
}

