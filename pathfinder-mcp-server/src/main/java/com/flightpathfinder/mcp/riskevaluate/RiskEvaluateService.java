package com.flightpathfinder.mcp.riskevaluate;

/**
 * Server-side contract for transfer-risk evaluation.
 *
 * <p>The risk tool stays inside the MCP server because it combines datasource lookups with local rule
 * scoring, and the bootstrap application only needs the MCP contract rather than the implementation.
 */
public interface RiskEvaluateService {

    /**
     * Scores the transfer scenario described by the query.
     *
     * @param query normalized risk-evaluation request
     * @return structured risk result with level, explanation and recommendations
     */
    RiskEvaluateResult evaluate(RiskEvaluateQuery query);
}
