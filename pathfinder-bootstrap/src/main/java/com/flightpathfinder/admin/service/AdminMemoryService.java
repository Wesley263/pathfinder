package com.flightpathfinder.admin.service;

/**
 * 持久化会话记忆的管理端查询服务。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
 * 避免在运维视图中直接暴露运行时记忆流水线的内部持久化模型。
 */
public interface AdminMemoryService {

    /**
        * 列出最近会话，或按条件精确查询单个会话。
     *
     * @param conversationId 参数说明。
     * @param limit 参数说明。
     * @return 返回结果。
     */
    AdminConversationListResult listConversations(String conversationId, int limit);

    /**
        * 加载单个会话的管理端详情。
     *
     * @param conversationId 参数说明。
     * @param recentMessageLimit 参数说明。
     * @return 返回结果。
     */
    AdminConversationDetailResult findConversationDetail(String conversationId, int recentMessageLimit);
}


