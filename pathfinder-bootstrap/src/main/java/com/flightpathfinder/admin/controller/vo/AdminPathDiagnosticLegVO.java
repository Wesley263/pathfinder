package com.flightpathfinder.admin.controller.vo;

/**
 * 管理端响应视图模型。
 */
public record AdminPathDiagnosticLegVO(
        String origin,
        String destination,
        String carrierCode,
        String carrierName,
        String carrierType,
        double priceCny,
        int durationMinutes,
        double distanceKm) {
}

