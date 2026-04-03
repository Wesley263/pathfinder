package com.flightpathfinder.rag.service.impl;

import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import com.flightpathfinder.rag.controller.vo.RagOverviewVO;
import com.flightpathfinder.rag.service.RagOverviewService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Default provider for the current RAG overview endpoint.
 *
 * <p>This service reports the current wiring as implemented facts, not future roadmap data,
 * and keeps controllers out of MCP registry details.</p>
 */
@Service
public class DefaultRagOverviewService implements RagOverviewService {

    private final LocalMcpToolRegistry localMcpToolRegistry;

    public DefaultRagOverviewService(LocalMcpToolRegistry localMcpToolRegistry) {
        this.localMcpToolRegistry = localMcpToolRegistry;
    }

    /**
     * Returns a compact overview of the currently wired RAG feature surface.
     *
     * @return overview payload for structure/operations introspection
     */
    @Override
    public RagOverviewVO currentOverview() {
        List<String> toolIds = localMcpToolRegistry.listTools().stream()
                .map(tool -> tool.toolId())
                .sorted()
                .toList();
        return new RagOverviewVO(
                "rag",
                "pathfinder-bootstrap",
                "implemented",
                toolIds.size(),
                List.of(
                        "mainline is rewrite -> intent classify -> kb/mcp split -> retrieval/tool execution -> final answer",
                        "client-side MCP registry currently exposes: " + String.join(", ", toolIds),
                        "conversation memory keeps recent turns through a feature-owned store; no summary or vector memory is active",
                        "trace is request-scoped and returned in audit data; trace persistence is not active",
                        "overview endpoint is static and reports current wiring only"
                ));
    }
}
