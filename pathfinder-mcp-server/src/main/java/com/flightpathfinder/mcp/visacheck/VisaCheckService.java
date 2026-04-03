package com.flightpathfinder.mcp.visacheck;

/**
 * Server-side contract for visa policy evaluation.
 *
 * <p>The MCP server keeps this rule evaluation local so {@code visa.check} can query visa policy data and
 * apply business rules without depending on bootstrap-side services.
 */
public interface VisaCheckService {

    /**
     * Evaluates visa requirements for the requested destinations and passport context.
     *
     * @param query normalized visa-check request
     * @return structured visa result including business-level missing-data states
     */
    VisaCheckResult check(VisaCheckQuery query);
}
