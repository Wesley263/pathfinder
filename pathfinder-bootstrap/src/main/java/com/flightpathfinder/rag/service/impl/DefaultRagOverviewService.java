package com.flightpathfinder.rag.service.impl;

import com.flightpathfinder.rag.core.mcp.LocalMcpToolRegistry;
import com.flightpathfinder.rag.controller.vo.RagOverviewVO;
import com.flightpathfinder.rag.service.RagOverviewService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 当前 RAG 概览接口的默认实现。
 *
 * <p>它报告的是“当前已经实现的事实”，而不是未来蓝图，同时让 controller 不需要直接接触 MCP registry 细节。</p>
 */
@Service
public class DefaultRagOverviewService implements RagOverviewService {

    /** 本地 MCP 工具注册表，用于统计当前已接入工具。 */
    private final LocalMcpToolRegistry localMcpToolRegistry;

    /**
     * 构造概览服务。
     *
     * @param localMcpToolRegistry 本地 MCP 工具注册表
     */
    public DefaultRagOverviewService(LocalMcpToolRegistry localMcpToolRegistry) {
        this.localMcpToolRegistry = localMcpToolRegistry;
    }

    /**
     * 返回当前已接入功能面的概览。
     *
     * @return 面向结构与运维查看的概览对象
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
