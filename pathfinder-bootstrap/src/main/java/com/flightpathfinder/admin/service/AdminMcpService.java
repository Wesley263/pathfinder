package com.flightpathfinder.admin.service;

/**
 * 管理端 MCP 工具目录查询服务。
 *
 * 对外提供工具清单与单工具详情，支持按需触发目录刷新。
 */
public interface AdminMcpService {

    /**
          * 查询管理端 MCP 工具清单。
     *
      * @param refresh 是否先刷新远端目录再返回结果
      * @return MCP 工具清单结果
     */
    AdminMcpToolListResult listTools(boolean refresh);

    /**
          * 查询单个 MCP 工具详情。
     *
      * @param toolId 目标工具标识
      * @param refresh 是否先刷新远端目录再查询
      * @return 工具详情结果
     */
    AdminMcpToolDetailResult findTool(String toolId, boolean refresh);
}
