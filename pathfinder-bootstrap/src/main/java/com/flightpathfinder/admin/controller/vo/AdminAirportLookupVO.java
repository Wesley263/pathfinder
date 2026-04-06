package com.flightpathfinder.admin.controller.vo;

import java.time.Instant;

/**
 * 管理端响应视图模型。
 */
public record AdminAirportLookupVO(
        String iataCode,
        String status,
        String message,
        Instant checkedAt,
        AdminAirportSummaryVO airport) {
}

