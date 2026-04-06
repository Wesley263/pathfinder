package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.model.RestoredGraphNode;
import com.flightpathfinder.mcp.graph.model.RestoredPathLeg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 图路径搜索质量检查的内部诊断运行器。
 *
 * 说明。
 * 说明。
 */
public final class GraphPathSearchDiagnostics {

    private GraphPathSearchDiagnostics() {
    }

    /**
        * 说明。
     *
        * @param args 未使用的命令行参数
     */
    public static void main(String[] args) {
        List<DiagnosticCase> diagnosticCases = List.of(
                legacyBalancedCase(),
                frontierPressureCase(),
                rankingStabilityCase());

        BoundedBestFirstGraphPathSearchService boundedSearch = new BoundedBestFirstGraphPathSearchService();
        DiagnosticOracle oracle = new DiagnosticOracle();

        System.out.println("== Graph Path Search Diagnostics ==");
        for (DiagnosticCase diagnosticCase : diagnosticCases) {
            List<RestoredCandidatePath> bounded = boundedSearch.search(
                    diagnosticCase.graph(),
                    diagnosticCase.request());
            List<RestoredCandidatePath> exhaustive = oracle.search(
                    diagnosticCase.graph(),
                    diagnosticCase.request());
            DiagnosticComparison comparison = compare(bounded, exhaustive, diagnosticCase.request().topK());

            System.out.println("-- " + diagnosticCase.name() + " --");
            System.out.println("bounded signatures   : " + signaturesOf(bounded));
            System.out.println("exhaustive signatures: " + signaturesOf(exhaustive));
            System.out.println("top1 match           : " + comparison.top1Match());
            System.out.println("topK overlap         : " + comparison.overlap() + "/" + comparison.comparedTopK());
            System.out.println("missing from bounded : " + comparison.missingFromBounded());
        }
    }

    private static DiagnosticComparison compare(List<RestoredCandidatePath> bounded,
                                                List<RestoredCandidatePath> exhaustive,
                                                int topK) {
        List<String> boundedSignatures = signaturesOf(bounded);
        List<String> exhaustiveSignatures = signaturesOf(exhaustive);
        Set<String> boundedSet = new LinkedHashSet<>(boundedSignatures);
        Set<String> exhaustiveTop = new LinkedHashSet<>(exhaustiveSignatures.subList(0, Math.min(topK, exhaustiveSignatures.size())));
        Set<String> overlap = new LinkedHashSet<>(boundedSet);
        overlap.retainAll(exhaustiveTop);
        Set<String> missing = new LinkedHashSet<>(exhaustiveTop);
        missing.removeAll(boundedSet);
        boolean top1Match = !boundedSignatures.isEmpty()
                && !exhaustiveSignatures.isEmpty()
                && boundedSignatures.get(0).equals(exhaustiveSignatures.get(0));
        return new DiagnosticComparison(
                top1Match,
                overlap.size(),
                Math.min(topK, exhaustiveTop.size()),
                List.copyOf(missing));
    }

    private static List<String> signaturesOf(List<RestoredCandidatePath> paths) {
        return paths.stream().map(GraphPathSearchDiagnostics::signatureOf).toList();
    }

    private static String signatureOf(RestoredCandidatePath path) {
        return path.legs().stream()
                .map(leg -> leg.origin() + "-" + leg.destination() + ":" + leg.carrierCode())
                .reduce((left, right) -> left + " | " + right)
                .orElse("<empty>");
    }

    private static DiagnosticCase legacyBalancedCase() {
        RestoredFlightGraph.Builder builder = RestoredFlightGraph.builder();
        addNode(builder, "PVG", 31.1, 121.8, 60);
        addNode(builder, "NRT", 35.7, 140.4, 90);
        addNode(builder, "LAX", 33.9, -118.4, 120);
        addNode(builder, "SFO", 37.6, -122.4, 120);
        addNode(builder, "YVR", 49.2, -123.2, 80);

        addEdge(builder, "PVG", "NRT", "CA", 2500, 180, 1830, 0.85, true, 3, 0);
        addEdge(builder, "PVG", "NRT", "NH", 2800, 175, 1830, 0.90, true, 3, 0);
        addEdge(builder, "NRT", "LAX", "UA", 8000, 540, 8750, 0.80, true, 2, 0);
        addEdge(builder, "NRT", "LAX", "DL", 7500, 530, 8750, 0.82, true, 2, 0);
        addEdge(builder, "PVG", "LAX", "CA", 12000, 720, 10450, 0.85, true, 1, 0);
        addEdge(builder, "PVG", "SFO", "UA", 11000, 680, 9890, 0.80, true, 1, 0);
        addEdge(builder, "SFO", "LAX", "AA", 800, 90, 540, 0.88, true, 5, 0);
        addEdge(builder, "PVG", "YVR", "AC", 8600, 620, 9030, 0.84, true, 1, 0);
        addEdge(builder, "YVR", "LAX", "AC", 1200, 170, 1730, 0.86, true, 3, 0);

        return new DiagnosticCase(
                "legacy-balanced",
                builder.build(),
                new GraphPathSearchRequest("diag", "PVG", "LAX", 15000, 0, 3, 5));
    }

    private static DiagnosticCase frontierPressureCase() {
        RestoredFlightGraph.Builder builder = RestoredFlightGraph.builder();
        addNode(builder, "AAA", 30.0, 120.0, 50);
        addNode(builder, "ZZZ", 40.0, -73.0, 80);
        for (int i = 0; i < 18; i++) {
            String hub = "H" + (char) ('A' + i) + (char) ('A' + i);
            addNode(builder, hub, 31.0 + i * 0.2, 121.0 + i * 1.5, 70);
            addEdge(builder, "AAA", hub, "C" + i, 1800 + i * 90, 140 + i * 8, 900 + i * 35, 0.80 + (i % 4) * 0.03, i % 3 != 0, 2 + (i % 5), i % 2);
            addEdge(builder, hub, "ZZZ", "D" + i, 2100 + i * 70, 260 + i * 10, 1400 + i * 40, 0.78 + (i % 5) * 0.03, i % 4 != 0, 2 + (i % 4), i % 2);
        }

        addNode(builder, "QAA", 38.0, 135.0, 75);
        addNode(builder, "QBB", 41.0, 150.0, 75);
        addEdge(builder, "AAA", "QAA", "Q1", 1700, 130, 880, 0.91, true, 6, 0);
        addEdge(builder, "QAA", "QBB", "Q2", 1500, 185, 1050, 0.93, true, 4, 0);
        addEdge(builder, "QBB", "ZZZ", "Q3", 1400, 210, 1260, 0.92, true, 5, 0);

        addEdge(builder, "AAA", "ZZZ", "DX", 5200, 540, 9600, 0.79, true, 1, 0);

        return new DiagnosticCase(
                "frontier-pressure",
                builder.build(),
                new GraphPathSearchRequest("diag", "AAA", "ZZZ", 9000, 0, 4, 5));
    }

    private static DiagnosticCase rankingStabilityCase() {
        RestoredFlightGraph.Builder builder = RestoredFlightGraph.builder();
        addNode(builder, "SRC", 34.0, 108.0, 60);
        addNode(builder, "MID", 36.0, 121.0, 80);
        addNode(builder, "ALT", 38.0, 128.0, 80);
        addNode(builder, "DST", 40.0, 139.0, 90);

        addEdge(builder, "SRC", "DST", "D1", 5100, 430, 2600, 0.86, true, 1, 0);
        addEdge(builder, "SRC", "MID", "M1", 2100, 160, 940, 0.91, true, 6, 0);
        addEdge(builder, "MID", "DST", "M2", 2400, 210, 1260, 0.89, true, 5, 0);
        addEdge(builder, "SRC", "ALT", "A1", 1700, 145, 880, 0.77, false, 3, 1);
        addEdge(builder, "ALT", "DST", "A2", 1600, 175, 1010, 0.80, false, 3, 1);
        addEdge(builder, "SRC", "MID", "M3", 1900, 175, 940, 0.84, false, 2, 1);
        addEdge(builder, "MID", "DST", "M4", 1800, 250, 1260, 0.83, false, 2, 1);

        return new DiagnosticCase(
                "ranking-stability",
                builder.build(),
                new GraphPathSearchRequest("diag", "SRC", "DST", 7000, 0, 3, 5));
    }

    private static void addNode(RestoredFlightGraph.Builder builder,
                                String airportCode,
                                double latitude,
                                double longitude,
                                int minTransferMinutes) {
        builder.addNode(new RestoredGraphNode(
                airportCode,
                airportCode,
                airportCode,
                "XX",
                latitude,
                longitude,
                "UTC",
                minTransferMinutes));
    }

    private static void addEdge(RestoredFlightGraph.Builder builder,
                                String origin,
                                String destination,
                                String carrierCode,
                                double price,
                                int durationMinutes,
                                double distanceKm,
                                double onTimeRate,
                                boolean baggageIncluded,
                                int competitionCount,
                                int stops) {
        builder.addEdge(new RestoredGraphEdge(
                origin + "-" + destination + "-" + carrierCode,
                origin,
                destination,
                carrierCode,
                carrierCode,
                "FSC",
                price,
                durationMinutes,
                distanceKm,
                onTimeRate,
                baggageIncluded,
                competitionCount,
                stops));
    }

    private record DiagnosticCase(String name,
                                  RestoredFlightGraph graph,
                                  GraphPathSearchRequest request) {
    }

    private record DiagnosticComparison(boolean top1Match,
                                        int overlap,
                                        int comparedTopK,
                                        List<String> missingFromBounded) {
    }

    private static final class DiagnosticOracle {

        private final GraphPathParetoFilter paretoFilter = new GraphPathParetoFilter();
        private final GraphPathWeightedRanker weightedRanker = new GraphPathWeightedRanker();

        List<RestoredCandidatePath> search(RestoredFlightGraph graph, GraphPathSearchRequest request) {
            GraphPathSearchLowerBounds lowerBounds = GraphPathSearchLowerBounds.prepare(
                    graph,
                    request.origin(),
                    request.destination());
            GraphPathSearchProfile profile = GraphPathSearchProfile.defaultFor(request);
            List<RestoredCandidatePath> rawCandidates = new ArrayList<>();

            Set<String> visited = new HashSet<>();
            visited.add(request.origin());
            enumerate(
                    graph,
                    request,
                    lowerBounds,
                    profile,
                    GraphPathPartialState.root(request.origin()),
                    new ArrayList<>(),
                    visited,
                    rawCandidates);

            GraphPathParetoSelection selection = paretoFilter.selectForRanking(
                    rawCandidates,
                    profile.frontierPolicy().candidatePoolSize());
            return weightedRanker.rank(graph, selection, profile.scoringProfile(), request.topK());
        }

        private void enumerate(RestoredFlightGraph graph,
                               GraphPathSearchRequest request,
                               GraphPathSearchLowerBounds lowerBounds,
                               GraphPathSearchProfile profile,
                               GraphPathPartialState state,
                               List<RestoredGraphEdge> edges,
                               Set<String> visited,
                               List<RestoredCandidatePath> paths) {
            GraphPathSearchHeuristic heuristic = new GraphPathSearchHeuristic(graph, request, lowerBounds, profile);
            if (state.currentAirport().equals(request.destination()) && !edges.isEmpty()) {
                paths.add(toCandidatePath(edges, state));
                return;
            }
            if (state.segmentsUsed() >= request.maxSegments()) {
                return;
            }
            if (heuristic.shouldPruneByDetour(state)) {
                return;
            }
            int remainingSegments = request.maxSegments() - state.segmentsUsed();
            if (!lowerBounds.canReachWithinSegments(state.currentAirport(), remainingSegments)
                    || !lowerBounds.canReachWithinBudget(state.currentAirport(), state.totalPriceCny(), request.maxBudget())) {
                return;
            }

            int transferMinutes = graph.getNode(state.currentAirport()).minTransferMinutes();
            int stopoverMinutes = request.stopoverDays() * 24 * 60;
            for (RestoredGraphEdge edge : graph.getOutgoingEdges(state.currentAirport())) {
                if (visited.contains(edge.destination())) {
                    continue;
                }
                GraphPathPartialState nextState = state.advance(edge, transferMinutes, stopoverMinutes);
                int nextRemainingSegments = request.maxSegments() - nextState.segmentsUsed();
                if (nextState.totalPriceCny() > request.maxBudget()
                        || heuristic.shouldPruneByDetour(nextState)
                        || !lowerBounds.canReachWithinSegments(nextState.currentAirport(), nextRemainingSegments)
                        || !lowerBounds.canReachWithinBudget(nextState.currentAirport(), nextState.totalPriceCny(), request.maxBudget())) {
                    continue;
                }
                visited.add(edge.destination());
                edges.add(edge);
                enumerate(graph, request, lowerBounds, profile, nextState, edges, visited, paths);
                edges.remove(edges.size() - 1);
                visited.remove(edge.destination());
            }
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
    }
}
