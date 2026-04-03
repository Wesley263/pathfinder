package com.flightpathfinder.mcp.citycost;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public record CityCostQuery(List<String> iataCodes) {

    public CityCostQuery {
        LinkedHashSet<String> normalizedCodes = new LinkedHashSet<>();
        for (String iataCode : iataCodes == null ? List.<String>of() : iataCodes) {
            String normalized = normalize(iataCode);
            if (!normalized.isBlank()) {
                normalizedCodes.add(normalized);
            }
        }
        iataCodes = List.copyOf(normalizedCodes);
        if (iataCodes.isEmpty()) {
            throw new IllegalArgumentException("iataCodes is required");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
