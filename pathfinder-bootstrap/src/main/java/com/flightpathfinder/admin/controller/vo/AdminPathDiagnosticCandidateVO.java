package com.flightpathfinder.admin.controller.vo;

import java.util.List;

public record AdminPathDiagnosticCandidateVO(
        int segmentCount,
        int transferCount,
        double totalPriceCny,
        int totalDurationMinutes,
        double totalDistanceKm,
        double averageOnTimeRate,
        List<String> hubAirports,
        List<AdminPathDiagnosticLegVO> legs) {
}
