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
 * Bootstrap-side read-only query service for published graph snapshots.
 *
 * <p>This service exists mainly for admin/inspection use cases. It reads the same Redis
 * read model that the MCP server consumes, which keeps snapshot visibility aligned across
 * processes.</p>
 */
@Service
public class RedisGraphSnapshotQueryService implements GraphSnapshotQueryService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final GraphSnapshotProperties graphSnapshotProperties;

    public RedisGraphSnapshotQueryService(StringRedisTemplate stringRedisTemplate,
                                          ObjectMapper objectMapper,
                                          GraphSnapshotProperties graphSnapshotProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    /**
     * Loads the current published snapshot for one graph key from Redis.
     *
     * @param graphKey logical graph identifier
     * @return current snapshot when present
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
        // Admin and diagnostics read the same Redis snapshot that MCP consumes so management views match the
        // actual graph read model seen by path-search execution.
        return Optional.of(snapshot);
    }

    private GraphSnapshot deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, GraphSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR, "failed to deserialize current graph snapshot payload");
        }
    }

    private String normalizeGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey.trim();
    }
}
