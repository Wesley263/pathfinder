package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredGraphNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.ToDoubleFunction;

/**
 * 图路径搜索使用的预计算下界。
 *
 * 说明。
 * 说明。
 */
final class GraphPathSearchLowerBounds {

    /** 不可达价格哨兵值。 */
    private static final double UNREACHABLE_PRICE = Double.POSITIVE_INFINITY;
    /** 不可达时长哨兵值。 */
    private static final int UNREACHABLE_DURATION = Integer.MAX_VALUE;
    /** 不可达航段哨兵值。 */
    private static final int UNREACHABLE_SEGMENTS = Integer.MAX_VALUE;

    /** 机场到最小剩余航段数。 */
    private final Map<String, Integer> minRemainingSegments;
    /** 机场到最小剩余价格。 */
    private final Map<String, Double> minRemainingPriceCny;
    /** 机场到最小剩余时长。 */
    private final Map<String, Integer> minRemainingDurationMinutes;
    /** 机场到终点直线距离。 */
    private final Map<String, Double> straightLineDistanceToDestinationKm;
    /** 起终点直线距离。 */
    private final double directDistanceOriginToDestinationKm;

    /**
     * 构造下界对象。
     */
    private GraphPathSearchLowerBounds(Map<String, Integer> minRemainingSegments,
                                       Map<String, Double> minRemainingPriceCny,
                                       Map<String, Integer> minRemainingDurationMinutes,
                                       Map<String, Double> straightLineDistanceToDestinationKm,
                                       double directDistanceOriginToDestinationKm) {
        this.minRemainingSegments = Map.copyOf(minRemainingSegments);
        this.minRemainingPriceCny = Map.copyOf(minRemainingPriceCny);
        this.minRemainingDurationMinutes = Map.copyOf(minRemainingDurationMinutes);
        this.straightLineDistanceToDestinationKm = Map.copyOf(straightLineDistanceToDestinationKm);
        this.directDistanceOriginToDestinationKm = directDistanceOriginToDestinationKm;
    }

    /**
     * 为一次搜索预计算航段、价格、时长与直线距离下界。
     */
    static GraphPathSearchLowerBounds prepare(RestoredFlightGraph graph, String origin, String destination) {
        Map<String, List<RestoredGraphEdge>> incomingEdges = buildIncomingEdges(graph);
        Map<String, Integer> minSegments = computeMinSegments(incomingEdges, destination);
        Map<String, Double> minPrice = computeMinDoubleCosts(incomingEdges, destination, RestoredGraphEdge::basePriceCny);
        Map<String, Integer> minDuration = computeMinIntCosts(incomingEdges, destination, RestoredGraphEdge::durationMinutes);
        Map<String, Double> straightLineToDestination = computeStraightLineDistances(graph, destination);
        double directDistance = straightLineToDestination.getOrDefault(origin, 0.0);
        return new GraphPathSearchLowerBounds(minSegments, minPrice, minDuration, straightLineToDestination, directDistance);
    }

    /**
     * 判断在剩余航段预算内是否仍可到达终点。
     */
    boolean canReachWithinSegments(String airportCode, int remainingSegments) {
        return minRemainingSegments(airportCode) <= remainingSegments;
    }

    /**
     * 判断当前已花费与乐观剩余花费之和是否仍在预算内。
     */
    boolean canReachWithinBudget(String airportCode, double spentPriceCny, double maxBudgetCny) {
        return spentPriceCny + minRemainingPriceCny(airportCode) <= maxBudgetCny;
    }

    /**
     * 返回机场最小剩余航段数。
     */
    int minRemainingSegments(String airportCode) {
        return minRemainingSegments.getOrDefault(airportCode, UNREACHABLE_SEGMENTS);
    }

    /**
     * 返回机场最小剩余价格。
     */
    double minRemainingPriceCny(String airportCode) {
        return minRemainingPriceCny.getOrDefault(airportCode, UNREACHABLE_PRICE);
    }

    /**
     * 返回机场最小剩余时长。
     */
    int minRemainingDurationMinutes(String airportCode) {
        return minRemainingDurationMinutes.getOrDefault(airportCode, UNREACHABLE_DURATION);
    }

    /**
     * 估计当前路径相对起终点直连的绕路比例。
     */
    double estimatedDetourRatio(String airportCode, double travelledDistanceKm) {
        if (directDistanceOriginToDestinationKm <= 0.0) {
            return 1.0;
        }
        double estimatedTotalDistance = travelledDistanceKm
                + straightLineDistanceToDestinationKm.getOrDefault(airportCode, 0.0);
        return estimatedTotalDistance / directDistanceOriginToDestinationKm;
    }

    /**
     * 构建反向入边索引。
     */
    private static Map<String, List<RestoredGraphEdge>> buildIncomingEdges(RestoredFlightGraph graph) {
        Map<String, List<RestoredGraphEdge>> incomingEdges = new HashMap<>();
        for (String airportCode : graph.getAllNodes()) {
            for (RestoredGraphEdge edge : graph.getOutgoingEdges(airportCode)) {
                incomingEdges.computeIfAbsent(edge.destination(), ignored -> new ArrayList<>()).add(edge);
            }
        }
        return incomingEdges;
    }

    /**
     * 说明。
     */
    private static Map<String, Integer> computeMinSegments(Map<String, List<RestoredGraphEdge>> incomingEdges,
                                                           String destination) {
        Map<String, Integer> minSegments = new HashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        minSegments.put(destination, 0);
        queue.add(destination);
        // 说明。
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            int nextDistance = minSegments.get(current) + 1;
            for (RestoredGraphEdge incomingEdge : incomingEdges.getOrDefault(current, List.of())) {
                String previous = incomingEdge.origin();
                Integer known = minSegments.get(previous);
                if (known == null || nextDistance < known) {
                    minSegments.put(previous, nextDistance);
                    queue.addLast(previous);
                }
            }
        }
        return minSegments;
    }

    /**
     * 说明。
     */
    private static Map<String, Double> computeMinDoubleCosts(Map<String, List<RestoredGraphEdge>> incomingEdges,
                                                             String destination,
                                                             ToDoubleFunction<RestoredGraphEdge> costFn) {
        Map<String, Double> costs = new HashMap<>();
        PriorityQueue<GraphNodeDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(GraphNodeDistance::cost));
        costs.put(destination, 0.0);
        queue.add(new GraphNodeDistance(destination, 0.0));
        while (!queue.isEmpty()) {
            GraphNodeDistance current = queue.poll();
            if (current.cost() > costs.getOrDefault(current.airportCode(), Double.POSITIVE_INFINITY)) {
                continue;
            }
            for (RestoredGraphEdge incomingEdge : incomingEdges.getOrDefault(current.airportCode(), List.of())) {
                double nextCost = current.cost() + costFn.applyAsDouble(incomingEdge);
                Double known = costs.get(incomingEdge.origin());
                if (known == null || nextCost < known) {
                    costs.put(incomingEdge.origin(), nextCost);
                    queue.add(new GraphNodeDistance(incomingEdge.origin(), nextCost));
                }
            }
        }
        return costs;
    }

    /**
     * 说明。
     */
    private static Map<String, Integer> computeMinIntCosts(Map<String, List<RestoredGraphEdge>> incomingEdges,
                                                           String destination,
                                                           java.util.function.ToIntFunction<RestoredGraphEdge> costFn) {
        Map<String, Integer> costs = new HashMap<>();
        PriorityQueue<GraphNodeDuration> queue = new PriorityQueue<>(Comparator.comparingInt(GraphNodeDuration::cost));
        costs.put(destination, 0);
        queue.add(new GraphNodeDuration(destination, 0));
        while (!queue.isEmpty()) {
            GraphNodeDuration current = queue.poll();
            if (current.cost() > costs.getOrDefault(current.airportCode(), Integer.MAX_VALUE)) {
                continue;
            }
            for (RestoredGraphEdge incomingEdge : incomingEdges.getOrDefault(current.airportCode(), List.of())) {
                int nextCost = current.cost() + costFn.applyAsInt(incomingEdge);
                Integer known = costs.get(incomingEdge.origin());
                if (known == null || nextCost < known) {
                    costs.put(incomingEdge.origin(), nextCost);
                    queue.add(new GraphNodeDuration(incomingEdge.origin(), nextCost));
                }
            }
        }
        return costs;
    }

    /**
     * 计算各节点到终点的直线距离。
     */
    private static Map<String, Double> computeStraightLineDistances(RestoredFlightGraph graph, String destination) {
        RestoredGraphNode destinationNode = graph.getNode(destination);
        if (destinationNode == null) {
            return Map.of();
        }
        Map<String, Double> distances = new HashMap<>();
        for (String airportCode : graph.getAllNodes()) {
            RestoredGraphNode node = graph.getNode(airportCode);
            if (node == null) {
                continue;
            }
            distances.put(airportCode, haversineKm(
                    node.latitude(),
                    node.longitude(),
                    destinationNode.latitude(),
                    destinationNode.longitude()));
        }
        return distances;
    }

    /**
     * 计算两地大圆距离（公里）。
     */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    /** 注释说明。 */
    private record GraphNodeDistance(String airportCode, double cost) {
    }

    /** 注释说明。 */
    private record GraphNodeDuration(String airportCode, int cost) {
    }
}
