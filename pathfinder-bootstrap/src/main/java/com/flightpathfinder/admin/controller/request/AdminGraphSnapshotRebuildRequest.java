package com.flightpathfinder.admin.controller.request;

public record AdminGraphSnapshotRebuildRequest(
        String graphKey,
        String reason) {
}
