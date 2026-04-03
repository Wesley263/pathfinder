package com.flightpathfinder.mcp.citycost;

/**
 * Server-side contract for city cost lookup.
 *
 * <p>The MCP server owns this query path directly because {@code city.cost} reads its own dataset from the
 * datasource instead of delegating to shared bootstrap business modules.
 */
public interface CityCostService {

    /**
     * Looks up city cost entries for the requested airport or city codes.
     *
     * @param query normalized city-cost request
     * @return matched cost entries plus structured missing-city information
     */
    CityCostResult lookup(CityCostQuery query);
}
