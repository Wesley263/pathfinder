package com.flightpathfinder.flight.core.graph.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 图快照发布器。
 *
 * 负责持久化版本化快照载荷，并同步更新 latest 版本指针。
 */
@Component
public class RedisGraphSnapshotPublisher implements GraphSnapshotPublisher {

    /** Redis 字符串操作模板，用于写入快照键。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 快照序列化对象映射器。 */
    private final ObjectMapper objectMapper;
    /** 图快照配置。 */
    private final GraphSnapshotProperties graphSnapshotProperties;

    /**
        * 构造 Redis 图快照发布器。
     *
        * @param stringRedisTemplate Redis 字符串操作模板
        * @param objectMapper JSON 序列化对象映射器
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
      * 发布指定图标识的快照到 Redis。
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

          // 快照载荷与 latest 指针使用同一 TTL，避免出现版本指针悬挂。
        stringRedisTemplate.opsForValue().set(snapshotKey, payload, graphSnapshotProperties.getTtl());
        stringRedisTemplate.opsForValue().set(latestKey, snapshot.snapshotVersion(), graphSnapshotProperties.getTtl());
    }

    /**
      * 归一化图标识，缺省时回退到默认图标识。
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
        * @return 快照 JSON 载荷
     */
    private String toJson(GraphSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize graph snapshot", ex);
        }
    }
}

