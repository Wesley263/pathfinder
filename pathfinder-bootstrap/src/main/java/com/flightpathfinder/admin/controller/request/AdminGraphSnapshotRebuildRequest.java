package com.flightpathfinder.admin.controller.request;

/**
 * 管理端请求参数模型。
 */
public record AdminGraphSnapshotRebuildRequest(
        String graphKey,
        String reason) {
}

