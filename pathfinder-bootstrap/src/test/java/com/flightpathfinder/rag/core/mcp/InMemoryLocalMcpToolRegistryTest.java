package com.flightpathfinder.rag.core.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class InMemoryLocalMcpToolRegistryTest {

    @Test
    void replaceAllAndFind_shouldStoreImmutableSnapshotAndSupportLookup() {
        InMemoryLocalMcpToolRegistry registry = new InMemoryLocalMcpToolRegistry();
        List<McpToolDescriptor> tools = new ArrayList<>(List.of(
                new McpToolDescriptor("flight.search", "Flight Search", "desc", Map.of(), Map.of()),
                new McpToolDescriptor("price.lookup", "Price Lookup", "desc", Map.of(), Map.of())));

        registry.replaceAll(tools);
        tools.clear();

        assertEquals(2, registry.listTools().size());
        assertTrue(registry.findByToolId("flight.search").isPresent());
        assertTrue(registry.findByToolId("missing.tool").isEmpty());
    }
}



