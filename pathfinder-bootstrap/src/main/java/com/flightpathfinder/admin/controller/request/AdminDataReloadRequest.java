package com.flightpathfinder.admin.controller.request;

public record AdminDataReloadRequest(
        String graphKey,
        String reason) {
}
