package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import java.util.List;

/**
 * 恢复图路径规划搜索边界。
 *
 * <p>该服务仅对恢复后的内存图执行搜索。快照读取与 MCP 协议处理不在此边界内。</p>
 */
public interface GraphPathSearchService {

    /**
     * 在恢复图上搜索候选路径。
     *
     * @param graph 由已发布快照恢复的内存图
     * @param request 路径搜索约束
     * @return 与当前 MCP 契约兼容的排序候选路径
     */
    List<RestoredCandidatePath> search(RestoredFlightGraph graph, GraphPathSearchRequest request);
}
