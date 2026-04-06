package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredPathLeg;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 有界 best-first 图路径搜索服务。
 *
 * 使用 optimistic frontier、支配裁剪和候选池控制搜索规模，
 * 最终通过 Pareto 选择与加权排序输出 TopK 候选路径。
 */
@Service
public class BoundedBestFirstGraphPathSearchService implements GraphPathSearchService {

    /** Pareto 选择器。 */
    private final GraphPathParetoFilter paretoFilter = new GraphPathParetoFilter();
    /** 最终加权排序器。 */
    private final GraphPathWeightedRanker weightedRanker = new GraphPathWeightedRanker();

    /**
        * 执行有界 best-first 搜索。
     *
     * @param graph 由已发布快照恢复的图
     * @param request 路径搜索约束
        * @return 按排序后的候选路径列表
     */
    @Override
    public List<RestoredCandidatePath> search(RestoredFlightGraph graph, GraphPathSearchRequest request) {
        if (!graph.hasNode(request.origin()) || !graph.hasNode(request.destination())) {
            return List.of();
        }

        // 先用下界快速剪枝不可达起点，避免进入主循环。
        GraphPathSearchLowerBounds lowerBounds = GraphPathSearchLowerBounds.prepare(
                graph,
                request.origin(),
                request.destination());
        if (!lowerBounds.canReachWithinSegments(request.origin(), request.maxSegments())
                || !lowerBounds.canReachWithinBudget(request.origin(), 0.0, request.maxBudget())) {
            return List.of();
        }

        GraphPathSearchProfile profile = GraphPathSearchProfile.defaultFor(request);
        GraphPathSearchHeuristic heuristic = new GraphPathSearchHeuristic(graph, request, lowerBounds, profile);
        GraphPathExpansionFrontier expansionFrontier = new GraphPathExpansionFrontier();
        GraphPathPartialFrontier dominanceFrontier = new GraphPathPartialFrontier();
        GraphPathCandidatePool candidatePool = new GraphPathCandidatePool(profile.frontierPolicy().candidatePoolSize());

        long sequence = 0L;
        GraphPathPartialState rootState = GraphPathPartialState.root(request.origin());
        dominanceFrontier.record(rootState);
        expansionFrontier.offer(new GraphPathSearchNode(
                rootState,
                List.of(),
                heuristic.optimisticScore(rootState),
                sequence++));

        int expansions = 0;
        while (!expansionFrontier.isEmpty() && expansions < profile.frontierPolicy().maxExpansions()) {
            // 每轮优先扩展 optimisticScore 最高的节点。
            GraphPathSearchNode node = expansionFrontier.pollBest();
            if (node == null) {
                break;
            }
            if (node.state().currentAirport().equals(request.destination()) && !node.edges().isEmpty()) {
                // 命中终点后直接尝试准入候选池。
                admitCandidate(node.edges(), node.state(), heuristic, candidatePool);
                if (candidatePool.shouldStop(expansionFrontier.peekBestScore(), profile.frontierPolicy())) {
                    break;
                }
                continue;
            }
            // 已被更优状态支配的节点无需扩展。
            if (dominanceFrontier.isDominated(node.state())) {
                continue;
            }
            if (node.state().segmentsUsed() >= request.maxSegments()) {
                continue;
            }
            if (!isExpandable(node.state(), request, lowerBounds, heuristic)) {
                continue;
            }

            expansions++;
            expandNode(
                    graph,
                    request,
                    lowerBounds,
                    heuristic,
                    profile,
                    node,
                    dominanceFrontier,
                    expansionFrontier,
                    candidatePool,
                    sequence);
            sequence += Math.max(1, graph.getOutgoingEdges(node.state().currentAirport()).size());
            for (GraphPathSearchNode trimmedNode : expansionFrontier.trimToSize(profile.frontierPolicy().maxFrontierSize())) {
                // 被 frontier 淘汰的状态同步从支配索引移除。
                dominanceFrontier.discard(trimmedNode.state());
            }
            if (candidatePool.shouldStop(expansionFrontier.peekBestScore(), profile.frontierPolicy())) {
                break;
            }
        }

        List<RestoredCandidatePath> admittedCandidates = candidatePool.admittedCandidates();
        if (admittedCandidates.isEmpty()) {
            return List.of();
        }

        // 先做 Pareto 过滤，再做加权排序输出最终 topK。
        GraphPathParetoSelection selection = paretoFilter.selectForRanking(
                admittedCandidates,
                profile.frontierPolicy().minimumParetoSelectionCount());
        return weightedRanker.rank(graph, selection, profile.scoringProfile(), request.topK());
    }

    /**
         * 扩展单个节点并向 frontier 写入后继状态。
     */
    private void expandNode(RestoredFlightGraph graph,
                            GraphPathSearchRequest request,
                            GraphPathSearchLowerBounds lowerBounds,
                            GraphPathSearchHeuristic heuristic,
                            GraphPathSearchProfile profile,
                            GraphPathSearchNode node,
                            GraphPathPartialFrontier dominanceFrontier,
                            GraphPathExpansionFrontier expansionFrontier,
                            GraphPathCandidatePool candidatePool,
                            long sequenceBase) {
        int transferMinutes = graph.getNode(node.state().currentAirport()).minTransferMinutes();
        int stopoverMinutes = request.stopoverDays() * 24 * 60;

        List<GraphPathSearchNode> successors = new ArrayList<>();
        long sequenceOffset = 0L;
        for (RestoredGraphEdge edge : graph.getOutgoingEdges(node.state().currentAirport())) {
            if (node.state().visitedAirports().contains(edge.destination())) {
                continue;
            }

            GraphPathPartialState nextState = node.state().advance(edge, transferMinutes, stopoverMinutes);
            if (!isCandidateReachable(nextState, request, lowerBounds, heuristic)) {
                continue;
            }
            if (nextState.currentAirport().equals(request.destination())) {
                List<RestoredGraphEdge> candidateEdges = new ArrayList<>(node.edges().size() + 1);
                candidateEdges.addAll(node.edges());
                candidateEdges.add(edge);
                admitCandidate(candidateEdges, nextState, heuristic, candidatePool);
                continue;
            }

            double optimisticScore = heuristic.optimisticScore(nextState);
            if (candidatePool.shouldRejectPotential(
                    optimisticScore,
                    profile.frontierPolicy().candidateAdmissionSlack())
                    || dominanceFrontier.isDominated(nextState)) {
                continue;
            }

            dominanceFrontier.record(nextState);
            List<RestoredGraphEdge> nextEdges = new ArrayList<>(node.edges().size() + 1);
            nextEdges.addAll(node.edges());
            nextEdges.add(edge);
            successors.add(new GraphPathSearchNode(
                    nextState,
                    List.copyOf(nextEdges),
                    optimisticScore,
                    sequenceBase + sequenceOffset++));
        }

        successors.sort(java.util.Comparator
                .comparingDouble(GraphPathSearchNode::optimisticScore)
                .reversed()
                .thenComparingLong(GraphPathSearchNode::sequence));
        for (GraphPathSearchNode successor : successors) {
            expansionFrontier.offer(successor);
        }
    }

    /**
     * 判断当前状态是否可继续扩展。
     */
    private boolean isExpandable(GraphPathPartialState state,
                                 GraphPathSearchRequest request,
                                 GraphPathSearchLowerBounds lowerBounds,
                                 GraphPathSearchHeuristic heuristic) {
        int remainingSegments = request.maxSegments() - state.segmentsUsed();
        return lowerBounds.canReachWithinSegments(state.currentAirport(), remainingSegments)
                && lowerBounds.canReachWithinBudget(state.currentAirport(), state.totalPriceCny(), request.maxBudget())
                && !heuristic.shouldPruneByDetour(state);
    }

    /**
     * 判断候选后继状态是否仍可达并符合预算与绕路约束。
     */
    private boolean isCandidateReachable(GraphPathPartialState nextState,
                                         GraphPathSearchRequest request,
                                         GraphPathSearchLowerBounds lowerBounds,
                                         GraphPathSearchHeuristic heuristic) {
        int nextRemainingSegments = request.maxSegments() - nextState.segmentsUsed();
        return nextState.totalPriceCny() <= request.maxBudget()
                && lowerBounds.canReachWithinSegments(nextState.currentAirport(), nextRemainingSegments)
                && lowerBounds.canReachWithinBudget(nextState.currentAirport(), nextState.totalPriceCny(), request.maxBudget())
                && !heuristic.shouldPruneByDetour(nextState);
    }

    /**
     * 把边序列与部分状态转换为候选路径对象。
     */
    private RestoredCandidatePath toCandidatePath(List<RestoredGraphEdge> edges,
                                                  GraphPathPartialState state) {
        List<RestoredPathLeg> legs = edges.stream()
                .map(edge -> new RestoredPathLeg(
                        edge.origin(),
                        edge.destination(),
                        edge.carrierCode(),
                        edge.carrierName(),
                        edge.carrierType(),
                        edge.basePriceCny(),
                        edge.durationMinutes(),
                        edge.distanceKm(),
                        edge.onTimeRate(),
                        edge.baggageIncluded(),
                        edge.competitionCount(),
                        edge.stops()))
                .toList();

        return new RestoredCandidatePath(
                legs,
                state.totalPriceCny(),
                state.totalDurationMinutes(),
                state.totalDistanceKm(),
                state.averageOnTimeRate());
    }

    /**
     * 将候选路径按评分准入候选池。
     */
    private void admitCandidate(List<RestoredGraphEdge> edges,
                                GraphPathPartialState state,
                                GraphPathSearchHeuristic heuristic,
                                GraphPathCandidatePool candidatePool) {
        RestoredCandidatePath candidate = toCandidatePath(edges, state);
        candidatePool.admit(candidate, heuristic.candidateScore(candidate));
    }
}
