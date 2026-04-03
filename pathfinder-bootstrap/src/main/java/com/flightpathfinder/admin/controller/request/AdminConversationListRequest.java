package com.flightpathfinder.admin.controller.request;

public class AdminConversationListRequest {

    private String conversationId;
    private int limit = 20;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
