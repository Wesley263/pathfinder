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
 * 图快照生命周期服务默认实现。
 *
 * 说明。
 * 说明。
 */
@Service
public class DefaultGraphSnapshotLifecycleService implements GraphSnapshotLifecycleService {

    /** 图快照构建器。 */
    private final GraphSnapshotBuilder graphSnapshotBuilder;
    /** 图快照发布器。 */
    private final GraphSnapshotPublisher graphSnapshotPublisher;
    /** 注释说明。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 图快照配置。 */
    private final GraphSnapshotProperties graphSnapshotProperties;

    /**
     * 构造图快照生命周期服务。
     *
     * @param graphSnapshotBuilder 图快照构建器
     * @param graphSnapshotPublisher 图快照发布器
     * @param stringRedisTemplate 参数说明。
     * @param graphSnapshotProperties 图快照配置
     */
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
        * 说明。
     *
        * @param graphKey 图逻辑标识
     */
    @Override
    public void invalidate(String graphKey) {
        Set<String> keys = stringRedisTemplate.keys(GraphSnapshotRedisKeys.snapshotKeyPattern(normalizeGraphKey(graphKey)));
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
        * 说明。
     *
        * @param graphKey 图逻辑标识
        * @param reason 参数说明。
        * @return 已完成发布的重建快照
     */
    @Override
    public GraphSnapshot rebuild(String graphKey, String reason) {
        String normalizedGraphKey = normalizeGraphKey(graphKey);
        GraphSnapshot snapshot = graphSnapshotBuilder.build(normalizedGraphKey);
        Map<String, Object> metadata = new LinkedHashMap<>(snapshot.metadata());
        // 说明。
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
}

