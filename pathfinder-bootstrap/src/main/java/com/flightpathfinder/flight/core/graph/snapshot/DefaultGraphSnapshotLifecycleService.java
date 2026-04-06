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
 * 协调构建器与发布器，向上提供快照失效与重建能力。
 */
@Service
public class DefaultGraphSnapshotLifecycleService implements GraphSnapshotLifecycleService {

    /** 图快照构建器。 */
    private final GraphSnapshotBuilder graphSnapshotBuilder;
    /** 图快照发布器。 */
    private final GraphSnapshotPublisher graphSnapshotPublisher;
    /** Redis 字符串操作模板，用于删除历史快照键。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 图快照配置。 */
    private final GraphSnapshotProperties graphSnapshotProperties;

    /**
     * 构造图快照生命周期服务。
     *
     * @param graphSnapshotBuilder 图快照构建器
     * @param graphSnapshotPublisher 图快照发布器
    * @param stringRedisTemplate Redis 字符串操作模板
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
      * 删除指定图标识下的快照相关键。
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
      * 重新构建并发布指定图标识的快照。
     *
      * @param graphKey 图逻辑标识
      * @param reason 本次重建原因，空值时写入 unspecified
      * @return 已完成发布的重建快照
     */
    @Override
    public GraphSnapshot rebuild(String graphKey, String reason) {
        String normalizedGraphKey = normalizeGraphKey(graphKey);
        GraphSnapshot snapshot = graphSnapshotBuilder.build(normalizedGraphKey);
        Map<String, Object> metadata = new LinkedHashMap<>(snapshot.metadata());
          // 在元数据里补充重建原因，方便管理面定位操作来源。
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
        * 归一化图标识，缺省时回退到配置中的默认值。
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

