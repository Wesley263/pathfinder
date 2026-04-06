package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceContext;
import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Rag 运行链路追踪记录器默认实现。
 *
 * 基于框架 TraceContext 创建根节点并追加阶段节点，供后续持久化与可视化查询使用。
 */
@Service
public class DefaultRagTraceRecorder implements RagTraceRecorder {

    /**
        * 创建新的追踪根节点。
     *
        * @param scene 追踪场景标识
        * @return 新建的追踪根节点
     */
    @Override
    public TraceRoot startRoot(String scene) {
        return TraceContext.startRoot(scene);
    }

    /**
        * 向追踪根节点追加一个阶段节点。
     *
        * @param root 当前追踪根节点
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
     * 清理当前线程中的追踪上下文。
     */
    @Override
    public void clear() {
        TraceContext.clear();
    }
}
