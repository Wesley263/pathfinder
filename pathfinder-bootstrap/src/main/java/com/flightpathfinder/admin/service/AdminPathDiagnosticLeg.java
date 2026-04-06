package com.flightpathfinder.admin.service;

/**
 * 管理端服务层数据模型。
 */
public record AdminPathDiagnosticLeg(
        String origin,
        String destination,
        String carrierCode,
        String carrierName,
        String carrierType,
        double priceCny,
        int durationMinutes,
        double distanceKm) {
}

