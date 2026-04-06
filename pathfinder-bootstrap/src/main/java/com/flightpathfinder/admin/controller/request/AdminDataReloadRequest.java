package com.flightpathfinder.admin.controller.request;

/**
 * 管理端请求参数模型。
 */
public record AdminDataReloadRequest(
        String graphKey,
        String reason) {
}

