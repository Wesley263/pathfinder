package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import java.util.List;

/**
 * 恢复图路径规划搜索边界。
 *
 * 定义在恢复图上执行候选路径搜索的标准接口。
 */
public interface GraphPathSearchService {

    /**
     * 在恢复图上搜索候选路径。
     *
     * @param graph 由已发布快照恢复的内存图
     * @param request 路径搜索约束
     * @return 候选路径列表
     */
    List<RestoredCandidatePath> search(RestoredFlightGraph graph, GraphPathSearchRequest request);
}
