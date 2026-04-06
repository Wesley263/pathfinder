package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import java.time.Instant;
import java.util.Map;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 * 使记录策略与持久化策略可独立演进。
 */
public interface RagTraceRecorder {

    /**
     * 说明。
     *
     * @param scene 参数说明。
     * @return 返回结果。
     */
    TraceRoot startRoot(String scene);

    /**
     * 说明。
     *
     * @param root 参数说明。
     * @param name 节点名称，通常是阶段名或内部子步骤名
     * @param startedAt 节点开始时间
     * @param finishedAt 节点结束时间
     * @param attributes 结构化节点属性
     * @return 追加后的节点
     */
    TraceNode appendNode(TraceRoot root, String name, Instant startedAt, Instant finishedAt, Map<String, Object> attributes);

    /**
     * 说明。
     */
    void clear();
}
