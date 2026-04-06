package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

/**
 * 管理端响应视图模型。
 */
public record AdminDataStatsVO(
        long airportCount,
        long airlineCount,
        long routeCount,
        long visaPolicyCount,
        long cityCostCount,
        Instant queriedAt) {
}

