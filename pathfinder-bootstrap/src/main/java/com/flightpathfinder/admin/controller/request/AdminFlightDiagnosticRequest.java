package com.flightpathfinder.admin.controller.request;

/**
 * 管理端请求参数模型。
 */
public record AdminFlightDiagnosticRequest(
        String origin,
        String destination,
        String date,
        int flexibilityDays,
        int topK) {
}

