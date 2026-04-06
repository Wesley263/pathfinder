package com.flightpathfinder.flight.core.graph.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
@Component
public class RedisGraphSnapshotPublisher implements GraphSnapshotPublisher {

    /** 注释说明。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 快照序列化对象映射器。 */
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
    public RedisGraphSnapshotPublisher(StringRedisTemplate stringRedisTemplate,
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
        * @param snapshot 待发布快照
     */
    @Override
    public void publish(String graphKey, GraphSnapshot snapshot) {
        String normalizedGraphKey = normalizeGraphKey(graphKey);
        String latestKey = GraphSnapshotRedisKeys.latestVersionKey(normalizedGraphKey);
        String snapshotKey = GraphSnapshotRedisKeys.snapshotKey(normalizedGraphKey, snapshot.snapshotVersion());
        String payload = toJson(snapshot);

        // 说明。
        stringRedisTemplate.opsForValue().set(snapshotKey, payload, graphSnapshotProperties.getTtl());
        stringRedisTemplate.opsForValue().set(latestKey, snapshot.snapshotVersion(), graphSnapshotProperties.getTtl());
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
                : graphKey;
    }

    /**
     * 序列化快照对象。
     *
     * @param snapshot 图快照
     * @return 返回结果。
     */
    private String toJson(GraphSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize graph snapshot", ex);
        }
    }
}

