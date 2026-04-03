package com.flightpathfinder.mcp.pricelookup;

import com.flightpathfinder.mcp.flightsearch.FlightSearchOption;
import com.flightpathfinder.mcp.flightsearch.FlightSearchQuery;
import com.flightpathfinder.mcp.flightsearch.FlightSearchService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Default server-side implementation of {@link PriceLookupService}.
 *
 * <p>This service deliberately reuses {@link FlightSearchService} rather than duplicating route SQL. The
 * MCP contract still remains separate because price comparison needs pair-level coverage reporting and a
 * different result shape from plain flight search.
 */
@Service
public class DefaultPriceLookupService implements PriceLookupService {

    private final FlightSearchService flightSearchService;

    public DefaultPriceLookupService(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    /**
     * Resolves the lowest available option for each requested city pair.
     *
     * @param query normalized price-lookup request
     * @return matched price items plus missing pair information
     */
    @Override
    public PriceLookupResult lookup(PriceLookupQuery query) {
        List<PriceLookupItem> items = new ArrayList<>();
        LinkedHashSet<String> missingPairs = new LinkedHashSet<>();
        LinkedHashSet<String> requestedPairs = new LinkedHashSet<>();

        for (PriceLookupCityPair cityPair : query.cityPairs()) {
            requestedPairs.add(cityPair.pairKey());
            try {
                List<FlightSearchOption> options = flightSearchService.search(
                        new FlightSearchQuery(cityPair.origin(), cityPair.destination(), query.date(), 0, 1));
                // Partial coverage is a first-class business result for this tool, so individual pair misses
                // are recorded instead of failing the whole lookup.
                if (options.isEmpty()) {
                    missingPairs.add(cityPair.pairKey());
                    continue;
                }
                FlightSearchOption bestOption = options.getFirst();
                items.add(new PriceLookupItem(
                        cityPair.pairKey(),
                        cityPair.origin(),
                        cityPair.destination(),
                        bestOption.airlineCode(),
                        bestOption.airlineName(),
                        bestOption.airlineType(),
                        bestOption.date(),
                        bestOption.priceCny(),
                        bestOption.basePriceCny(),
                        bestOption.durationMinutes(),
                        bestOption.distanceKm(),
                        bestOption.lowCostCarrier()));
            } catch (IllegalArgumentException exception) {
                missingPairs.add(cityPair.pairKey());
            }
        }

        return new PriceLookupResult(
                query.date().toString(),
                List.copyOf(requestedPairs),
                items,
                List.copyOf(missingPairs));
    }
}
