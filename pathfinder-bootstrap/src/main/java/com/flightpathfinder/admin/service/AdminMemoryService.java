package com.flightpathfinder.admin.service;

/**
 * 持久化会话记忆的管理端查询服务。
 *
 * <p>该服务将记忆巡检能力限定在管理端，
 * 避免在运维视图中直接暴露运行时记忆流水线的内部持久化模型。
 */
public interface AdminMemoryService {

    /**
        * 列出最近会话，或按条件精确查询单个会话。
     *
     * @param conversationId optional exact conversation id filter
     * @param limit maximum number of conversations to return
     * @return admin-facing conversation list result
     */
    AdminConversationListResult listConversations(String conversationId, int limit);

    /**
        * 加载单个会话的管理端详情。
     *
     * @param conversationId exact conversation id
     * @param recentMessageLimit maximum number of recent messages to include
     * @return conversation detail result, including summary and recent messages when found
     */
    AdminConversationDetailResult findConversationDetail(String conversationId, int recentMessageLimit);
}
