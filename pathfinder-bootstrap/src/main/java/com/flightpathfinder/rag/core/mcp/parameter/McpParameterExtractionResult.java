package com.flightpathfinder.rag.core.mcp.parameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * 核心组件。
 */
public record McpParameterExtractionResult(
        String toolId,
        Map<String, Object> parameters,
        List<String> missingRequiredFields,
        String status,
        String errorMessage) {

    public McpParameterExtractionResult {
        toolId = toolId == null ? "" : toolId.trim();
        parameters = Map.copyOf(new LinkedHashMap<>(parameters == null ? Map.of() : parameters));
        missingRequiredFields = List.copyOf(missingRequiredFields == null ? List.of() : missingRequiredFields);
        status = status == null || status.isBlank() ? "UNKNOWN" : status;
        errorMessage = errorMessage == null ? "" : errorMessage.trim();
    }

    public boolean ready() {
        return "READY".equals(status);
    }
}

