package com.flightpathfinder.mcp.pricelookup;

import java.util.Locale;

public record PriceLookupCityPair(String origin, String destination) {

    public PriceLookupCityPair {
        origin = normalize(origin);
        destination = normalize(destination);
    }

    public String pairKey() {
        return origin + "," + destination;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
