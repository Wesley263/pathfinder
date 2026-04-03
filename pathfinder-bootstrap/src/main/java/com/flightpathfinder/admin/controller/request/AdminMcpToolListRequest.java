package com.flightpathfinder.admin.controller.request;

public class AdminMcpToolListRequest {

    private boolean refresh = true;

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }
}
