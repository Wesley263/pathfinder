package com.flightpathfinder.admin.controller.vo;

public record AdminTraceToolVO(
        String toolId,
        String status,
        String message,
        boolean snapshotMiss,
        boolean error) {
}
