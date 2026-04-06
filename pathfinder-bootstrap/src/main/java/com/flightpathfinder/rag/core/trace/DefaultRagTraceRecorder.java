package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceContext;
import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 面向 RAG 的跟踪层到 framework trace 原语的默认桥接实现。
 *
 * <p>该实现将与 {@link TraceContext} 的集成面控制在最小范围，
 * 使上层 trace 生命周期逻辑聚焦请求语义而非底层上下文改写细节。
 */
@Service
public class DefaultRagTraceRecorder implements RagTraceRecorder {

    /**
     * 为指定场景启动 framework trace 根节点。
     *
     * @param scene trace 场景名
     * @return 已启动的 trace 根节点
     */
    @Override
    public TraceRoot startRoot(String scene) {
        return TraceContext.startRoot(scene);
    }

    /**
     * 向给定根节点追加一个 trace 节点。
     *
     * @param root 活动 trace 根节点
     * @param name 节点名
     * @param startedAt 节点开始时间
     * @param finishedAt 节点结束时间
     * @param attributes 结构化节点属性
     * @return 追加后的节点
     */
    @Override
    public TraceNode appendNode(TraceRoot root,
                                String name,
                                Instant startedAt,
                                Instant finishedAt,
                                Map<String, Object> attributes) {
        TraceNode node = new TraceNode(
                name == null ? "" : name.trim(),
                startedAt == null ? Instant.now() : startedAt,
                finishedAt == null ? Instant.now() : finishedAt,
                Map.copyOf(new LinkedHashMap<>(attributes == null ? Map.of() : attributes)));
        root.nodes().add(node);
        return node;
    }

    /**
     * 清理底层 framework trace 上下文。
     */
    @Override
    public void clear() {
        TraceContext.clear();
    }
}
