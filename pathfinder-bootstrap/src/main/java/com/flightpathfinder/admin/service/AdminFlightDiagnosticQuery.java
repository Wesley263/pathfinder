package com.flightpathfinder.admin.service;

/**
 * 管理端服务层数据模型。
 */
public record AdminFlightDiagnosticQuery(
        String origin,
        String destination,
        String date,
        int flexibilityDays,
        int topK) {
}

