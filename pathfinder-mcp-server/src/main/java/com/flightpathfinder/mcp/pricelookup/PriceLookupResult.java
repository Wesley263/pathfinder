package com.flightpathfinder.mcp.pricelookup;

import java.util.List;

public record PriceLookupResult(
        String date,
        List<String> requestedPairs,
        List<PriceLookupItem> items,
        List<String> missingPairs) {

    public PriceLookupResult {
        date = date == null ? "" : date.trim();
        requestedPairs = List.copyOf(requestedPairs == null ? List.of() : requestedPairs);
        items = List.copyOf(items == null ? List.of() : items);
        missingPairs = List.copyOf(missingPairs == null ? List.of() : missingPairs);
    }
}
