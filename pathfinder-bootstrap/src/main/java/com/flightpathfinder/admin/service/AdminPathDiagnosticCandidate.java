package com.flightpathfinder.admin.service;

import java.util.List;

/**
 * 管理端服务层数据模型。
 */
public record AdminPathDiagnosticCandidate(
        int segmentCount,
        int transferCount,
        double totalPriceCny,
        int totalDurationMinutes,
        double totalDistanceKm,
        double averageOnTimeRate,
        List<String> hubAirports,
        List<AdminPathDiagnosticLeg> legs) {

    public AdminPathDiagnosticCandidate {
        hubAirports = List.copyOf(hubAirports == null ? List.of() : hubAirports);
        legs = List.copyOf(legs == null ? List.of() : legs);
    }
}

