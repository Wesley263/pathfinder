package com.flightpathfinder.admin.service;

import java.time.Instant;

public record AdminDataStats(
        long airportCount,
        long airlineCount,
        long routeCount,
        long visaPolicyCount,
        long cityCostCount,
        Instant queriedAt) {
}
