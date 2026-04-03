package com.flightpathfinder.mcp.flightsearch;

import java.util.List;

/**
 * Server-side contract for direct flight lookup.
 *
 * <p>The service lives inside {@code pathfinder-mcp-server} because {@code flight.search} is owned as an
 * independent MCP tool backed by the server's own datasource access rather than shared bootstrap business
 * code.
 */
public interface FlightSearchService {

    /**
     * Finds direct-flight options for the requested airport pair and date window.
     *
     * @param query normalized flight-search request
     * @return ranked direct-flight options, or an empty list when no options match
     */
    List<FlightSearchOption> search(FlightSearchQuery query);
}
