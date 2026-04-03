package com.flightpathfinder.admin.service;

/**
 * Admin-assisted diagnostic query service.
 *
 * <p>These queries live on the admin surface because they are intended for verification, debugging and
 * operational inspection rather than end-user product workflows.
 */
public interface AdminDiagnosticsService {

    /**
     * Executes a direct flight diagnostic check.
     *
     * @param query structured diagnostic query for direct flight search
     * @return admin-facing flight diagnostic result
     */
    AdminFlightDiagnosticResult searchFlights(AdminFlightDiagnosticQuery query);

    /**
     * Executes a direct graph-path diagnostic check.
     *
     * @param query structured diagnostic query for graph path search
     * @return admin-facing path diagnostic result
     */
    AdminPathDiagnosticResult searchPaths(AdminPathDiagnosticQuery query);

    /**
     * Looks up one airport from the operational dataset.
     *
     * @param iataCode airport IATA code to inspect
     * @return admin-facing airport lookup result
     */
    AdminAirportLookupResult lookupAirport(String iataCode);
}
