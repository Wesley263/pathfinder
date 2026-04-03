package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredPathLeg;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Bounded best-first implementation of {@link GraphPathSearchService}.
 *
 * <p>This is the core search engine behind {@code graph.path.search}. It keeps frontier
 * expansion, pruning, candidate admission, Pareto filtering, and final ranking in one search
 * boundary instead of scattering those phases across protocol or snapshot code.</p>
 */
@Service
public class BoundedBestFirstGraphPathSearchService implements GraphPathSearchService {

    private final GraphPathParetoFilter paretoFilter = new GraphPathParetoFilter();
    private final GraphPathWeightedRanker weightedRanker = new GraphPathWeightedRanker();

    /**
     * Searches candidate paths on a restored graph using bounded best-first expansion.
     *
     * @param graph restored graph derived from the published snapshot
     * @param request path search constraints
     * @return ranked candidate paths compatible with the MCP response contract
     */
    @Override
    public List<RestoredCandidatePath> search(RestoredFlightGraph graph, GraphPathSearchRequest request) {
        if (!graph.hasNode(request.origin()) || !graph.hasNode(request.destination())) {
            return List.of();
        }

        // Lower bounds are prepared once and then reused by multiple pruning decisions so the
        // search loop can reject obviously bad states before the frontier grows too large.
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
            // Frontier expansion is best-first rather than DFS enumeration, so the search keeps
            // pulling the most promising partial state under the current heuristic.
            GraphPathSearchNode node = expansionFrontier.pollBest();
            if (node == null) {
                break;
            }
            if (node.state().currentAirport().equals(request.destination()) && !node.edges().isEmpty()) {
                // Candidate admission happens before more expansion so the pool can tighten
                // early-stop decisions using real completed paths instead of only heuristics.
                admitCandidate(node.edges(), node.state(), heuristic, candidatePool);
                if (candidatePool.shouldStop(expansionFrontier.peekBestScore(), profile.frontierPolicy())) {
                    break;
                }
                continue;
            }
            // Dominance pruning prevents the frontier from keeping a strictly worse partial
            // state that reaches the same airport with worse cost/reliability/visited history.
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
                // Frontier size is explicitly bounded so the search remains predictable and does
                // not degrade back into "expand everything, filter later".
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

        // Pareto filtering reduces the admitted pool to non-dominated layers before weighted
        // ranking picks the final top-K, which improves candidate diversity and stability.
        GraphPathParetoSelection selection = paretoFilter.selectForRanking(
                admittedCandidates,
                profile.frontierPolicy().minimumParetoSelectionCount());
        return weightedRanker.rank(graph, selection, profile.scoringProfile(), request.topK());
    }

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

    private boolean isExpandable(GraphPathPartialState state,
                                 GraphPathSearchRequest request,
                                 GraphPathSearchLowerBounds lowerBounds,
                                 GraphPathSearchHeuristic heuristic) {
        int remainingSegments = request.maxSegments() - state.segmentsUsed();
        return lowerBounds.canReachWithinSegments(state.currentAirport(), remainingSegments)
                && lowerBounds.canReachWithinBudget(state.currentAirport(), state.totalPriceCny(), request.maxBudget())
                && !heuristic.shouldPruneByDetour(state);
    }

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

    private void admitCandidate(List<RestoredGraphEdge> edges,
                                GraphPathPartialState state,
                                GraphPathSearchHeuristic heuristic,
                                GraphPathCandidatePool candidatePool) {
        RestoredCandidatePath candidate = toCandidatePath(edges, state);
        candidatePool.admit(candidate, heuristic.candidateScore(candidate));
    }
}
