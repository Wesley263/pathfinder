package com.flightpathfinder.mcp.riskevaluate;

import java.util.List;

public record RiskEvaluateResult(
        String hubAirport,
        String firstAirline,
        String secondAirline,
        double bufferHours,
        String riskLevel,
        double riskScore,
        double airlineRiskScore,
        double bufferRiskScore,
        double hubRiskScore,
        double suggestedBufferHours,
        String explanation,
        List<String> recommendations,
        boolean sameAirline,
        boolean dataComplete,
        List<String> missingInputs) {
}
