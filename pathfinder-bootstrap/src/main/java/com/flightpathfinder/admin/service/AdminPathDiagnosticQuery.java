package com.flightpathfinder.admin.service;

/**
 * 管理端服务层数据模型。
 */
public record AdminPathDiagnosticQuery(
        String graphKey,
        String origin,
        String destination,
        double maxBudget,
        int stopoverDays,
        int maxSegments,
        int topK) {
}

