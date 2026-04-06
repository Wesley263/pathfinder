package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.framework.trace.TraceContext;
import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
@Service
public class DefaultRagTraceRecorder implements RagTraceRecorder {

    /**
     * 说明。
     *
     * @param scene 参数说明。
     * @return 返回结果。
     */
    @Override
    public TraceRoot startRoot(String scene) {
        return TraceContext.startRoot(scene);
    }

    /**
     * 说明。
     *
     * @param root 参数说明。
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
     * 说明。
     */
    @Override
    public void clear() {
        TraceContext.clear();
    }
}
