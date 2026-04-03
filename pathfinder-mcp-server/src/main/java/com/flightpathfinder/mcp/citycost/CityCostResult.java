package com.flightpathfinder.mcp.citycost;

import java.util.List;

public record CityCostResult(
        List<String> requestedCities,
        List<CityCostItem> items,
        List<String> missingCities) {
}
