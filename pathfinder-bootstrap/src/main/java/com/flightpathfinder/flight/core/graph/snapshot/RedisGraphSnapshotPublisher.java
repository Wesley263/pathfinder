package com.flightpathfinder.flight.core.graph.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis publisher for graph snapshot read models.
 *
 * <p>Redis publication is separated from snapshot building because the snapshot is a shared
 * read model consumed by other processes, especially the MCP server, not just an in-process
 * bootstrap concern.</p>
 */
@Component
public class RedisGraphSnapshotPublisher implements GraphSnapshotPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final GraphSnapshotProperties graphSnapshotProperties;

    public RedisGraphSnapshotPublisher(StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       GraphSnapshotProperties graphSnapshotProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    /**
     * Publishes the snapshot payload and latest-version pointer into Redis.
     *
     * @param graphKey logical graph identifier
     * @param snapshot snapshot payload to publish
     */
    @Override
    public void publish(String graphKey, GraphSnapshot snapshot) {
        String normalizedGraphKey = normalizeGraphKey(graphKey);
        String latestKey = GraphSnapshotRedisKeys.latestVersionKey(normalizedGraphKey);
        String snapshotKey = GraphSnapshotRedisKeys.snapshotKey(normalizedGraphKey, snapshot.snapshotVersion());
        String payload = toJson(snapshot);

        // Snapshot payload and latest pointer are both versioned in Redis so readers can stay
        // strictly read-only and never coordinate with the builder process directly.
        stringRedisTemplate.opsForValue().set(snapshotKey, payload, graphSnapshotProperties.getTtl());
        stringRedisTemplate.opsForValue().set(latestKey, snapshot.snapshotVersion(), graphSnapshotProperties.getTtl());
    }

    private String normalizeGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey;
    }

    private String toJson(GraphSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize graph snapshot", ex);
        }
    }
}
