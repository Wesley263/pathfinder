package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphEdge;
import com.flightpathfinder.mcp.graph.search.fixture.TestGraphSnapshotFactory;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class GraphPathSearchHeuristicTest {

    @Test
    void optimisticScore_shouldDecreaseAfterHighCostDetourAdvance() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.detourGraphAtoC();
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "A", "C", 1500, 0, 3, 3);
        GraphPathSearchLowerBounds lowerBounds = GraphPathSearchLowerBounds.prepare(graph, "A", "C");
        GraphPathSearchProfile profile = GraphPathSearchProfile.defaultFor(request);
        GraphPathSearchHeuristic heuristic = new GraphPathSearchHeuristic(graph, request, lowerBounds, profile);

        GraphPathPartialState root = GraphPathPartialState.root("A");
        RestoredGraphEdge edgeToDetour = graph.getEdges("A", "X").getFirst();
        GraphPathPartialState detourState = root.advance(edgeToDetour, 0, 0);

        double rootScore = heuristic.optimisticScore(root);
        double detourScore = heuristic.optimisticScore(detourState);

        assertTrue(rootScore >= 0.0 && rootScore <= 1.0);
        assertTrue(detourScore >= 0.0 && detourScore <= 1.0);
        assertTrue(rootScore > detourScore);
    }

    @Test
    void candidateScore_shouldPreferCheaperFasterReliablePath() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.detourGraphAtoC();
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "A", "C", 1500, 0, 3, 3);
        GraphPathSearchLowerBounds lowerBounds = GraphPathSearchLowerBounds.prepare(graph, "A", "C");
        GraphPathSearchHeuristic heuristic = new GraphPathSearchHeuristic(
                graph,
                request,
                lowerBounds,
                GraphPathSearchProfile.defaultFor(request));

        RestoredCandidatePath directCandidate = TestGraphSnapshotFactory.candidatePathFromEdges(
                graph.getEdges("A", "B").getFirst(),
                graph.getEdges("B", "C").getFirst());
        RestoredCandidatePath detourCandidate = TestGraphSnapshotFactory.candidatePathFromEdges(
                graph.getEdges("A", "X").getFirst(),
                graph.getEdges("X", "Y").getFirst(),
                graph.getEdges("Y", "C").getFirst());

        assertTrue(heuristic.candidateScore(directCandidate) > heuristic.candidateScore(detourCandidate));
    }

    @Test
    void shouldPruneByDetour_shouldPruneOnlyAfterSecondSegmentWhenRatioTooLarge() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.detourGraphAtoC();
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "A", "C", 1500, 0, 3, 3);
        GraphPathSearchHeuristic heuristic = new GraphPathSearchHeuristic(
                graph,
                request,
                GraphPathSearchLowerBounds.prepare(graph, "A", "C"),
                GraphPathSearchProfile.defaultFor(request));

        GraphPathPartialState root = GraphPathPartialState.root("A");
        GraphPathPartialState oneSegmentDetour = root.advance(graph.getEdges("A", "X").getFirst(), 0, 0);
        GraphPathPartialState twoSegmentDetour = oneSegmentDetour.advance(graph.getEdges("X", "Y").getFirst(), 45, 0);
        GraphPathPartialState nearDirect = root
                .advance(graph.getEdges("A", "B").getFirst(), 0, 0)
                .advance(graph.getEdges("B", "C").getFirst(), 45, 0);

        assertFalse(heuristic.shouldPruneByDetour(root));
        assertFalse(heuristic.shouldPruneByDetour(oneSegmentDetour));
        assertFalse(heuristic.shouldPruneByDetour(nearDirect));
        assertTrue(heuristic.shouldPruneByDetour(twoSegmentDetour));
    }
}



