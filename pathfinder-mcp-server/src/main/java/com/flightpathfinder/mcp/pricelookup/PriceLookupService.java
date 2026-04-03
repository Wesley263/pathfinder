package com.flightpathfinder.mcp.pricelookup;

/**
 * Server-side contract for lowest-price comparison across multiple city pairs.
 *
 * <p>This service stays on the server side even though it reuses {@code flight.search} capability because
 * the exposed MCP tool still owns its own input contract and result semantics.
 */
public interface PriceLookupService {

    /**
     * Resolves the lowest available option for each requested city pair.
     *
     * @param query normalized lookup request
     * @return structured result including matched pairs and missing coverage
     */
    PriceLookupResult lookup(PriceLookupQuery query);
}
