package com.flightpathfinder.mcp.visacheck;

import java.util.List;
import java.util.Locale;

public record VisaCheckQuery(
        List<String> countryCodes,
        int stayDays,
        String passportCountry) {

    public VisaCheckQuery {
        countryCodes = List.copyOf(countryCodes == null ? List.of() : countryCodes.stream()
                .map(VisaCheckQuery::normalizeCode)
                .filter(code -> !code.isBlank())
                .distinct()
                .toList());
        passportCountry = normalizeCode(passportCountry);
        if (countryCodes.isEmpty()) {
            throw new IllegalArgumentException("countryCodes must contain at least one country");
        }
        if (passportCountry.isBlank()) {
            throw new IllegalArgumentException("passportCountry is required");
        }
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
