package com.flightpathfinder.admin.service;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface AdminMcpService {

    /**
        * 说明。
     *
     * @param refresh 参数说明。
     * @return 返回结果。
     */
    AdminMcpToolListResult listTools(boolean refresh);

    /**
        * 说明。
     *
     * @param toolId 参数说明。
     * @param refresh 参数说明。
     * @return 返回结果。
     */
    AdminMcpToolDetailResult findTool(String toolId, boolean refresh);
}
