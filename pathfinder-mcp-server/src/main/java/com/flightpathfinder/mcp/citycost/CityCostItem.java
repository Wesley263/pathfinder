package com.flightpathfinder.mcp.citycost;

public record CityCostItem(
        String iataCode,
        String city,
        String country,
        String countryCode,
        double dailyCostUsd,
        double accommodationUsd,
        double mealCostUsd,
        double transportationUsd) {
}
