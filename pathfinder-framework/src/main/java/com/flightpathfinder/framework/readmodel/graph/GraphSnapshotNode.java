package com.flightpathfinder.framework.readmodel.graph;

import java.util.Map;

public record GraphSnapshotNode(
        String nodeId,
        String airportCode,
        String airportName,
        String cityName,
        String countryCode,
        double latitude,
        double longitude,
        String timezone,
        boolean visaFreeCn,
        boolean transitVisaFree,
        double dailyCostUsd,
        int minTransferMinutes,
        Map<String, Object> attributes) {
}
