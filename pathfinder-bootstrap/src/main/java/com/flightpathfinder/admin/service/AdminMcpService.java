package com.flightpathfinder.admin.service;

/**
 * 面向 MCP 工具目录状态的管理端查询服务。
 *
 * <p>该服务将 MCP 工具以管理实体方式呈现，强调可用性与依赖提示，
 * 与仅需发现与执行的用户侧 RAG 流程关注点不同。
 */
public interface AdminMcpService {

    /**
        * 从当前目录状态列出受管 MCP 工具。
     *
     * @param refresh whether discovery should refresh the local catalog before listing
     * @return admin-facing MCP tool list result
     */
    AdminMcpToolListResult listTools(boolean refresh);

    /**
        * 加载单个受管 MCP 工具详情视图。
     *
     * @param toolId managed MCP tool id
     * @param refresh whether discovery should refresh the local catalog before lookup
     * @return admin-facing tool detail result
     */
    AdminMcpToolDetailResult findTool(String toolId, boolean refresh);
}
