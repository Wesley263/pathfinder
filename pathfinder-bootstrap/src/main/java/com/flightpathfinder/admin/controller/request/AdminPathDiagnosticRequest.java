package com.flightpathfinder.admin.controller.request;

/**
 * 管理端请求参数模型。
 */
public record AdminPathDiagnosticRequest(
        String graphKey,
        String origin,
        String destination,
        double maxBudget,
        int stopoverDays,
        int maxSegments,
        int topK) {
}

