package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

public record AdminDataStatsVO(
        long airportCount,
        long airlineCount,
        long routeCount,
        long visaPolicyCount,
        long cityCostCount,
        Instant queriedAt) {
}
