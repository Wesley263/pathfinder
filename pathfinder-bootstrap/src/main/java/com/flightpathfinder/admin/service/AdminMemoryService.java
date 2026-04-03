package com.flightpathfinder.admin.service;

/**
 * Admin-facing query service for persisted conversation memory.
 *
 * <p>This service keeps memory inspection on the management surface so operator views do not directly expose
 * the internal persistence models used by the runtime memory pipeline.
 */
public interface AdminMemoryService {

    /**
     * Lists recent conversations or one exact conversation when filtered.
     *
     * @param conversationId optional exact conversation id filter
     * @param limit maximum number of conversations to return
     * @return admin-facing conversation list result
     */
    AdminConversationListResult listConversations(String conversationId, int limit);

    /**
     * Loads admin-facing detail for one conversation.
     *
     * @param conversationId exact conversation id
     * @param recentMessageLimit maximum number of recent messages to include
     * @return conversation detail result, including summary and recent messages when found
     */
    AdminConversationDetailResult findConversationDetail(String conversationId, int recentMessageLimit);
}
