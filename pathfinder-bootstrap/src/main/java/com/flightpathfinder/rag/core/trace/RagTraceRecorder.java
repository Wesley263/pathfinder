package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import java.time.Instant;
import java.util.Map;

/**
 * Rag 链路追踪记录接口。
 *
 * 抽象根节点创建、阶段节点追加和上下文清理能力，
 * 使记录策略与持久化策略可独立演进。
 */
public interface RagTraceRecorder {

    /**
        * 启动一次追踪并创建根节点。
     *
        * @param scene 追踪场景标识
        * @return 当前请求对应的追踪根节点
     */
    TraceRoot startRoot(String scene);

    /**
        * 向追踪树中追加阶段节点。
     *
        * @param root 追踪根节点
     * @param name 节点名称，通常是阶段名或内部子步骤名
     * @param startedAt 节点开始时间
     * @param finishedAt 节点结束时间
     * @param attributes 结构化节点属性
     * @return 追加后的节点
     */
    TraceNode appendNode(TraceRoot root, String name, Instant startedAt, Instant finishedAt, Map<String, Object> attributes);

    /**
     * 清理当前线程中的追踪上下文。
     */
    void clear();
}
