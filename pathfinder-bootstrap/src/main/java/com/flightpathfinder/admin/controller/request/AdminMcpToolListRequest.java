package com.flightpathfinder.admin.controller.request;

/**
 * 管理端请求参数模型。
 */
public class AdminMcpToolListRequest {

    private boolean refresh = true;

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }
}

