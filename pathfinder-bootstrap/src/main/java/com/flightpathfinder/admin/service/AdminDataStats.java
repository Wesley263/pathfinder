package com.flightpathfinder.admin.service;

import java.time.Instant;

/**
 * 管理端服务层数据模型。
 */
public record AdminDataStats(
        long airportCount,
        long airlineCount,
        long routeCount,
        long visaPolicyCount,
        long cityCostCount,
        Instant queriedAt) {
}

