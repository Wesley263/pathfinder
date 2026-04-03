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
 * Redis-backed graph snapshot reader for the MCP server.
 *
 * <p>This reader stays strictly read-only. The MCP server must never rebuild the graph or
 * query bootstrap-owned tables on snapshot miss; it only consumes the published read model.</p>
 */
@Component
public class RedisGraphSnapshotReader implements GraphSnapshotReader {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisGraphSnapshotReader(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Loads the latest published snapshot for the supplied graph key from Redis.
     *
     * @param graphKey logical graph identifier
     * @return latest snapshot when present
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

    private GraphSnapshot deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, GraphSnapshot.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize graph snapshot payload", ex);
        }
    }
}
