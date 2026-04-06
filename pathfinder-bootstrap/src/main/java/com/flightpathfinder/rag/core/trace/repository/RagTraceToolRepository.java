package com.flightpathfinder.rag.core.trace.repository;

import java.util.List;

/**
 * 跟踪内 MCP 工具摘要仓储接口。
 *
 * <p>工具摘要与阶段节点分离存储，
 * 便于运营侧直接查看精简 MCP 视图，无需重复解析检索节点属性。
 */
public interface RagTraceToolRepository {

    /**
     * 替换指定 traceId 的全部工具摘要行。
     *
     * @param traceId 唯一 trace 标识
     * @param records 该 trace 的工具摘要记录集
     */
    void replaceForTrace(String traceId, List<PersistedRagTraceToolRecord> records);

    /**
     * 加载单个 trace 的全部工具摘要行。
     *
     * @param traceId 唯一 trace 标识
     * @return 按工具序号排序的工具摘要行
     */
    List<PersistedRagTraceToolRecord> findByTraceId(String traceId);
}
