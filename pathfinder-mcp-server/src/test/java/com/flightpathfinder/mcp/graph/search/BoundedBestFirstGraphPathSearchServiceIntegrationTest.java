package com.flightpathfinder.mcp.graph.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.search.fixture.TestGraphSnapshotFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class BoundedBestFirstGraphPathSearchServiceIntegrationTest {

    private final BoundedBestFirstGraphPathSearchService service = new BoundedBestFirstGraphPathSearchService();

    @Test
    void search_shouldReturnEmptyWhenOriginOrDestinationNotExists() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.branchingGraphAtoC();
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "Z", "C", 1000, 0, 3, 3);

        List<RestoredCandidatePath> result = service.search(graph, request);

        assertTrue(result.isEmpty());
    }

    @Test
    void search_shouldReturnEmptyWhenDestinationDisconnected() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.disconnectedGraph();
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "A", "E", 5000, 0, 4, 3);

        List<RestoredCandidatePath> result = service.search(graph, request);

        assertTrue(result.isEmpty());
    }

    @Test
    void search_shouldKeepOnlyBudgetFeasiblePath() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.branchingGraphAtoC();
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "A", "C", 245, 0, 2, 5);

        List<RestoredCandidatePath> result = service.search(graph, request);

        assertEquals(1, result.size());
        RestoredCandidatePath only = result.getFirst();
        assertEquals(240.0, only.totalPriceCny(), 1.0e-9);
        assertEquals(2, only.segmentCount());
        assertEquals("A", only.legs().getFirst().origin());
        assertEquals("C", only.legs().getLast().destination());
    }

    @Test
    void search_shouldRespectMaxSegmentsAndPruneLargeDetourPath() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.detourGraphAtoC();
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "A", "C", 5000, 0, 2, 3);

        List<RestoredCandidatePath> result = service.search(graph, request);

        assertEquals(1, result.size());
        RestoredCandidatePath direct = result.getFirst();
        assertEquals(2, direct.segmentCount());
        assertEquals(210.0, direct.totalPriceCny(), 1.0e-9);
        assertFalse(direct.hubAirports().contains("X"));
        assertFalse(direct.hubAirports().contains("Y"));
    }

    @Test
    void search_shouldRespectTopK() {
        RestoredFlightGraph graph = TestGraphSnapshotFactory.branchingGraphAtoC();
        GraphPathSearchRequest request = new GraphPathSearchRequest("g", "A", "C", 1000, 0, 2, 1);

        List<RestoredCandidatePath> result = service.search(graph, request);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().segmentCount() <= 2);
    }
}

