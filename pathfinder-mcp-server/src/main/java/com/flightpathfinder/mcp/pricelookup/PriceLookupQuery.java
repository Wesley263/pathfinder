package com.flightpathfinder.mcp.pricelookup;

import java.time.LocalDate;
import java.util.List;

public record PriceLookupQuery(
        LocalDate date,
        List<PriceLookupCityPair> cityPairs) {

    public PriceLookupQuery {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        cityPairs = List.copyOf(cityPairs == null ? List.of() : cityPairs);
        if (cityPairs.isEmpty()) {
            throw new IllegalArgumentException("at least one city pair is required");
        }
    }
}
