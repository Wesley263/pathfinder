package com.flightpathfinder.mcp.visacheck;

public record VisaCheckItem(
        String countryCode,
        String countryName,
        String visaStatus,
        int maxStayDays,
        boolean transitFree,
        Integer transitMaxHours,
        String notes) {
}
