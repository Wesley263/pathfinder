package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredPathLeg;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * {@link GraphPathSearchService} 的 bounded best-first 实现。
 *
 * <p>这是 {@code graph.path.search} 背后的核心搜索引擎。
 * 它在单一搜索边界内完成 frontier 扩展、裁剪、候选准入、Pareto 过滤和最终排序，
 * 避免这些阶段分散到协议层或快照层。</p>
 */
@Service
public class BoundedBestFirstGraphPathSearchService implements GraphPathSearchService {

    /** Pareto 分层过滤器。 */
    private final GraphPathParetoFilter paretoFilter = new GraphPathParetoFilter();
    /** 最终加权排序器。 */
    private final GraphPathWeightedRanker weightedRanker = new GraphPathWeightedRanker();

    /**
     * 在恢复图上使用 bounded best-first 扩展搜索候选路径。
     *
     * @param graph 由已发布快照恢复的图
     * @param request 路径搜索约束
     * @return 与 MCP 响应契约兼容的排序候选路径
     */
    @Override
    public List<RestoredCandidatePath> search(RestoredFlightGraph graph, GraphPathSearchRequest request) {
        if (!graph.hasNode(request.origin()) || !graph.hasNode(request.destination())) {
            return List.of();
        }

        // 下界仅预计算一次并复用于多种裁剪判定，避免 frontier 膨胀后再做迟后过滤。
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
            // 这里按 best-first 而非 DFS 枚举，从 frontier 中持续取当前最有希望的部分状态。
            GraphPathSearchNode node = expansionFrontier.pollBest();
            if (node == null) {
                break;
            }
            if (node.state().currentAirport().equals(request.destination()) && !node.edges().isEmpty()) {
                // 先做候选准入再继续扩展，使 early-stop 基于真实完成路径而不只依赖启发式估计。
                admitCandidate(node.edges(), node.state(), heuristic, candidatePool);
                if (candidatePool.shouldStop(expansionFrontier.peekBestScore(), profile.frontierPolicy())) {
                    break;
                }
                continue;
            }
            // 支配裁剪可移除同机场下严格劣势状态，防止其在 frontier 中占位。
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
                // frontier 显式限长，避免退化为“先全扩展、后统一过滤”的不可控模式。
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

        // 先做 Pareto 过滤保留非支配层，再做加权排序选 top-K，能提升结果多样性与稳定性。
        GraphPathParetoSelection selection = paretoFilter.selectForRanking(
                admittedCandidates,
                profile.frontierPolicy().minimumParetoSelectionCount());
        return weightedRanker.rank(graph, selection, profile.scoringProfile(), request.topK());
    }

    /**
     * 扩展单个搜索节点并把后继状态放入 frontier。
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
