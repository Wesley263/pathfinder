package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Default lifecycle service for graph snapshots.
 *
 * <p>This service owns invalidate/rebuild operations on the bootstrap side so admin and ETL
 * flows can manage snapshot lifecycle without duplicating build or publish logic.</p>
 */
@Service
public class DefaultGraphSnapshotLifecycleService implements GraphSnapshotLifecycleService {

    private final GraphSnapshotBuilder graphSnapshotBuilder;
    private final GraphSnapshotPublisher graphSnapshotPublisher;
    private final StringRedisTemplate stringRedisTemplate;
    private final GraphSnapshotProperties graphSnapshotProperties;

    public DefaultGraphSnapshotLifecycleService(GraphSnapshotBuilder graphSnapshotBuilder,
                                                GraphSnapshotPublisher graphSnapshotPublisher,
                                                StringRedisTemplate stringRedisTemplate,
                                                GraphSnapshotProperties graphSnapshotProperties) {
        this.graphSnapshotBuilder = graphSnapshotBuilder;
        this.graphSnapshotPublisher = graphSnapshotPublisher;
        this.stringRedisTemplate = stringRedisTemplate;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    /**
     * Invalidates all published snapshot keys for the supplied graph key.
     *
     * @param graphKey logical graph identifier
     */
    @Override
    public void invalidate(String graphKey) {
        Set<String> keys = stringRedisTemplate.keys(GraphSnapshotRedisKeys.snapshotKeyPattern(normalizeGraphKey(graphKey)));
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * Rebuilds the snapshot from bootstrap-owned data and republishes it to Redis.
     *
     * @param graphKey logical graph identifier
     * @param reason operational reason recorded into metadata
     * @return rebuilt snapshot that has already been published
     */
    @Override
    public GraphSnapshot rebuild(String graphKey, String reason) {
        String normalizedGraphKey = normalizeGraphKey(graphKey);
        GraphSnapshot snapshot = graphSnapshotBuilder.build(normalizedGraphKey);
        Map<String, Object> metadata = new LinkedHashMap<>(snapshot.metadata());
        // Lifecycle metadata is appended here because rebuild reason is operational context,
        // not part of the builder's pure "read DB -> materialize snapshot" responsibility.
        metadata.put("rebuildReason", reason == null || reason.isBlank() ? "unspecified" : reason);
        GraphSnapshot enrichedSnapshot = new GraphSnapshot(
                snapshot.schemaVersion(),
                snapshot.snapshotVersion(),
                snapshot.generatedAt(),
                snapshot.graphKey(),
                snapshot.sourceFingerprint(),
                snapshot.nodes(),
                snapshot.edges(),
                Map.copyOf(metadata));
        graphSnapshotPublisher.publish(normalizedGraphKey, enrichedSnapshot);
        return enrichedSnapshot;
    }

    private String normalizeGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey;
    }
}
