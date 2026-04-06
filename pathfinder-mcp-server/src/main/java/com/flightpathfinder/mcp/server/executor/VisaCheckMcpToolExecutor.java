package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.visacheck.VisaCheckItem;
import com.flightpathfinder.mcp.visacheck.VisaCheckQuery;
import com.flightpathfinder.mcp.visacheck.VisaCheckResult;
import com.flightpathfinder.mcp.visacheck.VisaCheckService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 说明。
 *
 * 说明。
 * 引导侧只需要契约与结果，不需要规则引擎实现。
 */
@Component
public class VisaCheckMcpToolExecutor implements McpToolExecutor {

    private static final String TOOL_ID = "visa.check";
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");

    private final VisaCheckService visaCheckService;

    public VisaCheckMcpToolExecutor(VisaCheckService visaCheckService) {
        this.visaCheckService = visaCheckService;
    }

    /**
     * 说明。
     *
     * @return 返回结果。
     */
    @Override
    public McpToolDescriptor descriptor() {
        return new McpToolDescriptor(
                TOOL_ID,
                "Visa Check",
                "Checks visa-free, transit-free, required or missing-data status for destination countries using visa policy data in the MCP server.",
                Map.of(
                        "type", "object",
                        "required", List.of("countryCodes"),
                        "properties", Map.of(
                                "countryCodes", Map.of("type", "string"),
                                "stayDays", Map.of("type", "integer", "default", 0),
                                "passportCountry", Map.of("type", "string", "default", "CN")
                        )),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "passportCountry", Map.of("type", "string"),
                                "stayDays", Map.of("type", "integer"),
                                "status", Map.of("type", "string"),
                                "message", Map.of("type", "string"),
                                "resultCount", Map.of("type", "integer"),
                                "results", Map.of("type", "array")
                        )));
    }

    /**
     * 对请求目的地执行签证政策评估。
     *
     * @param request 参数说明。
     * @return 返回结果。
     */
    @Override
    public McpToolCallResult execute(McpToolCallRequest request) {
        try {
            VisaCheckQuery query = parseRequest(request.arguments());
            VisaCheckResult result = visaCheckService.check(query);
            String status = resolveStatus(result);
            return new McpToolCallResult(
                    TOOL_ID,
                    true,
                    buildMessage(status, result),
                    successBody(result, status, buildMessage(status, result)),
                    null);
        } catch (IllegalArgumentException exception) {
            return invalidRequestResult(exception.getMessage());
        } catch (Exception exception) {
            return new McpToolCallResult(
                    TOOL_ID,
                    false,
                    null,
                    Map.of(
                            "status", "EXECUTION_ERROR",
                            "message", "visa check failed unexpectedly",
                            "retryable", false,
                            "suggestedAction", "NONE",
                            "resultCount", 0,
                            "results", List.of()),
                    exception.getMessage());
        }
    }

    private VisaCheckQuery parseRequest(Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String countryCodes = stringValue(safeArguments.get("countryCodes"));
        String passportCountry = stringValue(safeArguments.get("passportCountry"));
        int stayDays = intValue(safeArguments.get("stayDays"), 0);
        if (countryCodes == null || countryCodes.isBlank()) {
            throw new IllegalArgumentException("countryCodes is required");
        }
        List<String> countries = parseCountryCodes(countryCodes);
        if (countries.isEmpty()) {
            throw new IllegalArgumentException("countryCodes must contain at least one 2-letter ISO country code");
        }
        String normalizedPassportCountry = passportCountry == null || passportCountry.isBlank()
                ? "CN"
                : passportCountry.trim().toUpperCase(Locale.ROOT);
        if (!COUNTRY_CODE_PATTERN.matcher(normalizedPassportCountry).matches()) {
            throw new IllegalArgumentException("passportCountry must be a 2-letter ISO country code");
        }
        if (stayDays < 0) {
            throw new IllegalArgumentException("stayDays cannot be negative");
        }
        return new VisaCheckQuery(countries, stayDays, normalizedPassportCountry);
    }

    private List<String> parseCountryCodes(String countryCodes) {
        List<String> codes = new ArrayList<>();
        for (String rawCode : countryCodes.split("[,;\\s]+")) {
            String normalized = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
            if (!COUNTRY_CODE_PATTERN.matcher(normalized).matches()) {
                continue;
            }
            // 说明。
            if (!codes.contains(normalized)) {
                codes.add(normalized);
            }
        }
        return List.copyOf(codes);
    }

    private String resolveStatus(VisaCheckResult result) {
        boolean hasFoundData = result.items().stream().anyMatch(item -> !"DATA_NOT_FOUND".equals(item.visaStatus()));
        boolean hasMissingData = result.items().stream().anyMatch(item -> "DATA_NOT_FOUND".equals(item.visaStatus()));
        if (!hasFoundData && hasMissingData) {
            return "DATA_NOT_FOUND";
        }
        if (hasFoundData && hasMissingData) {
            return "PARTIAL_SUCCESS";
        }
        return "SUCCESS";
    }

    private String buildMessage(String status, VisaCheckResult result) {
        return switch (status) {
            case "DATA_NOT_FOUND" -> "visa policy data is not available for the requested countries";
            case "PARTIAL_SUCCESS" -> "visa check completed with partial policy coverage";
            default -> "visa check completed";
        };
    }

    private Map<String, Object> successBody(VisaCheckResult result, String status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("passportCountry", result.passportCountry());
        body.put("stayDays", result.stayDays());
        body.put("status", status);
        body.put("message", message);
        body.put("retryable", false);
        body.put("suggestedAction", "NONE");
        body.put("resultCount", result.items().size());
        body.put("results", result.items().stream().map(this::toResultMap).toList());
        return Map.copyOf(body);
    }

    private Map<String, Object> toResultMap(VisaCheckItem item) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("countryCode", item.countryCode());
        body.put("countryName", item.countryName());
        body.put("visaStatus", item.visaStatus());
        body.put("maxStayDays", item.maxStayDays());
        body.put("transitFree", item.transitFree());
        body.put("transitMaxHours", item.transitMaxHours());
        body.put("notes", item.notes());
        return Map.copyOf(body);
    }

    private McpToolCallResult invalidRequestResult(String message) {
        return new McpToolCallResult(
                TOOL_ID,
                false,
                null,
                Map.of(
                        "status", "INVALID_REQUEST",
                        "message", message,
                        "retryable", false,
                        "suggestedAction", "FIX_REQUEST",
                        "resultCount", 0,
                        "results", List.of()),
                message);
    }

    private String stringValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = String.valueOf(rawValue).trim();
        return value.isEmpty() ? null : value;
    }

    private int intValue(Object rawValue, int defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(rawValue));
    }
}
