package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import java.time.Instant;
import java.util.Map;

/**
 * 框架 trace 原语的低层桥接接口。
 *
 * <p>该契约对上层 trace 服务屏蔽
 * {@link com.flightpathfinder.framework.trace.TraceContext} 的直接操作，
 * 使记录策略与持久化策略可独立演进。
 */
public interface RagTraceRecorder {

    /**
     * 为指定场景启动新的 trace 根节点。
     *
     * @param scene trace 场景名，例如 RAG chat 主链
     * @return 绑定当前请求作用域的新 trace 根节点
     */
    TraceRoot startRoot(String scene);

    /**
     * 向活动 trace 根节点追加一个节点。
     *
     * @param root 活动 trace 根节点
     * @param name 节点名称，通常是阶段名或内部子步骤名
     * @param startedAt 节点开始时间
     * @param finishedAt 节点结束时间
     * @param attributes 结构化节点属性
     * @return 追加后的节点
     */
    TraceNode appendNode(TraceRoot root, String name, Instant startedAt, Instant finishedAt, Map<String, Object> attributes);

    /**
     * 清理当前请求作用域的 trace 上下文。
     */
    void clear();
}
