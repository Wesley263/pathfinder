package com.flightpathfinder.mcp.graph;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotEdge;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredGraphNode;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 把已发布快照读模型恢复为内存搜索图。
 *
 * 说明。
 * 说明。
 */
@Component
public class GraphSnapshotRestorer {

    /**
     * 从已发布快照载荷恢复可搜索图。
     *
     * @param snapshot 已发布图快照读模型
     * @return 路径搜索使用的内存图表示
     */
    public RestoredFlightGraph restore(GraphSnapshot snapshot) {
        RestoredFlightGraph.Builder builder = RestoredFlightGraph.builder();
        Set<String> nodeIds = new HashSet<>();
        snapshot.nodes().forEach(node -> builder.addNode(new RestoredGraphNode(
                node.airportCode(),
                node.airportName(),
                node.cityName(),
                node.countryCode(),
                node.latitude(),
                node.longitude(),
                node.timezone(),
                node.minTransferMinutes()
        )));
        snapshot.nodes().forEach(node -> nodeIds.add(node.airportCode()));

        for (GraphSnapshotEdge edge : snapshot.edges()) {
            // 只有端点都可恢复的边才会保留，避免快照漂移或脏数据引入断链引用。
            if (!nodeIds.contains(edge.fromNodeId()) || !nodeIds.contains(edge.toNodeId())) {
                continue;
            }
            builder.addEdge(new RestoredGraphEdge(
                    edge.edgeId(),
                    edge.fromNodeId(),
                    edge.toNodeId(),
                    edge.carrierCode(),
                    edge.carrierName(),
                    edge.carrierType(),
                    edge.basePriceCny(),
                    edge.durationMinutes(),
                    edge.distanceKm(),
                    edge.onTimeRate(),
                    edge.baggageIncluded(),
                    edge.competitionCount(),
                    edge.stops()
            ));
        }

        return builder.build();
    }
}
