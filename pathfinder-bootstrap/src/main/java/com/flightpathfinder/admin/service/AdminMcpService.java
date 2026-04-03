package com.flightpathfinder.admin.service;

/**
 * Admin-facing query service for MCP tool catalog state.
 *
 * <p>This service presents MCP tools as management entities with availability and dependency hints, which is a
 * different concern from the user-facing RAG pipeline that only needs discovery and execution.
 */
public interface AdminMcpService {

    /**
     * Lists managed MCP tools from the current catalog state.
     *
     * @param refresh whether discovery should refresh the local catalog before listing
     * @return admin-facing MCP tool list result
     */
    AdminMcpToolListResult listTools(boolean refresh);

    /**
     * Loads one managed MCP tool detail view.
     *
     * @param toolId managed MCP tool id
     * @param refresh whether discovery should refresh the local catalog before lookup
     * @return admin-facing tool detail result
     */
    AdminMcpToolDetailResult findTool(String toolId, boolean refresh);
}
