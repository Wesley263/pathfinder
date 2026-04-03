package com.flightpathfinder.admin.controller.request;

public record AdminCacheInvalidateRequest(
        String graphKey,
        String reason) {
}
