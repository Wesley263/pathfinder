package com.flightpathfinder.mcp.graph.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内存恢复后的航旅图。
 *
 * <p>该对象是 path search 的直接输入，不依赖数据库或 Redis 访问。</p>
 */
public class RestoredFlightGraph {

    /** 节点索引：airportCode -> 节点。 */
    private final Map<String, RestoredGraphNode> nodes;
    /** 边索引：origin -> destination -> 边列表。 */
    private final Map<String, Map<String, List<RestoredGraphEdge>>> edges;

    /**
     * 构造恢复图。
     *
     * @param nodes 节点索引
     * @param edges 边索引
     */
    public RestoredFlightGraph(Map<String, RestoredGraphNode> nodes,
                               Map<String, Map<String, List<RestoredGraphEdge>>> edges) {
        this.nodes = Map.copyOf(nodes);
        this.edges = Map.copyOf(edges);
    }

    /**
     * 判断节点是否存在。
     *
     * @param airportCode 机场代码
     * @return 存在返回 true
     */
    public boolean hasNode(String airportCode) {
        return nodes.containsKey(airportCode);
    }

    /**
     * 按机场代码读取节点。
     *
     * @param airportCode 机场代码
     * @return 节点对象
     */
    public RestoredGraphNode getNode(String airportCode) {
        return nodes.get(airportCode);
    }

    /**
     * 查询相邻机场集合。
     *
     * @param airportCode 起点机场
     * @return 相邻机场集合
     */
    public Set<String> getAdjacentNodes(String airportCode) {
        return edges.getOrDefault(airportCode, Map.of()).keySet();
    }

    /**
     * 查询 origin 到 destination 的边列表。
     *
     * @param origin 起点机场
     * @param destination 终点机场
     * @return 边列表
     */
    public List<RestoredGraphEdge> getEdges(String origin, String destination) {
        return edges.getOrDefault(origin, Map.of()).getOrDefault(destination, List.of());
    }

    /**
     * 返回全部节点代码。
     *
     * @return 节点代码集合
     */
    public Set<String> getAllNodes() {
        return nodes.keySet();
    }

    /**
     * 查询一个机场的所有出边。
     *
     * @param airportCode 机场代码
     * @return 出边列表
     */
    public List<RestoredGraphEdge> getOutgoingEdges(String airportCode) {
        return edges.getOrDefault(airportCode, Map.of()).values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 恢复图构建器。
     */
    public static class Builder {

        /** 构建中的节点索引。 */
        private final Map<String, RestoredGraphNode> nodes = new HashMap<>();
        /** 构建中的边索引。 */
        private final Map<String, Map<String, List<RestoredGraphEdge>>> edges = new HashMap<>();

        /**
         * 添加节点。
         *
         * @param node 节点
         * @return 构建器自身
         */
        public Builder addNode(RestoredGraphNode node) {
            nodes.put(node.airportCode(), node);
            edges.putIfAbsent(node.airportCode(), new HashMap<>());
            return this;
        }

        /**
         * 添加边。
         *
         * @param edge 边
         * @return 构建器自身
         */
        public Builder addEdge(RestoredGraphEdge edge) {
            edges.computeIfAbsent(edge.origin(), ignored -> new HashMap<>())
                    .computeIfAbsent(edge.destination(), ignored -> new java.util.ArrayList<>())
                    .add(edge);
            return this;
        }

        /**
         * 构建不可变恢复图。
         *
         * @return 恢复图
         */
        public RestoredFlightGraph build() {
            return new RestoredFlightGraph(nodes, edges);
        }
    }
}
