package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class GraphPathExpansionFrontierTest {

    @Test
    void offerAndPoll_shouldRespectScoreThenSequenceOrdering() {
        GraphPathExpansionFrontier frontier = new GraphPathExpansionFrontier();
        frontier.offer(node("A", 0.80, 2));
        frontier.offer(node("B", 0.80, 1));
        frontier.offer(node("C", 0.90, 3));

        assertEquals(0.90, frontier.peekBestScore(), 1.0e-9);
        assertEquals("C", frontier.pollBest().state().currentAirport());
        assertEquals("B", frontier.pollBest().state().currentAirport());
        assertEquals("A", frontier.pollBest().state().currentAirport());
        assertNull(frontier.pollBest());
        assertEquals(Double.NEGATIVE_INFINITY, frontier.peekBestScore());
    }

    @Test
    void trimToSize_shouldRemoveWorstNodesAndKeepBestOnes() {
        GraphPathExpansionFrontier frontier = new GraphPathExpansionFrontier();
        frontier.offer(node("A", 0.70, 1));
        frontier.offer(node("B", 0.50, 2));
        frontier.offer(node("C", 0.60, 3));
        frontier.offer(node("D", 0.40, 4));

        List<GraphPathSearchNode> trimmed = frontier.trimToSize(2);

        assertEquals(2, trimmed.size());
        assertEquals("D", trimmed.get(0).state().currentAirport());
        assertEquals("B", trimmed.get(1).state().currentAirport());
        assertEquals("A", frontier.pollBest().state().currentAirport());
        assertEquals("C", frontier.pollBest().state().currentAirport());
    }

    private static GraphPathSearchNode node(String airportCode, double optimisticScore, long sequence) {
        return new GraphPathSearchNode(GraphPathPartialState.root(airportCode), List.of(), optimisticScore, sequence);
    }
}



