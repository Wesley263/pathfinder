package com.flightpathfinder.mcp.visacheck;

import java.util.List;

public record VisaCheckResult(
        String passportCountry,
        int stayDays,
        List<VisaCheckItem> items) {

    public VisaCheckResult {
        passportCountry = passportCountry == null ? "" : passportCountry.trim();
        items = List.copyOf(items == null ? List.of() : items);
    }
}
