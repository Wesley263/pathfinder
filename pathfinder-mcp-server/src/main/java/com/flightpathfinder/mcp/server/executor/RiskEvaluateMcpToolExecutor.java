package com.flightpathfinder.mcp.server.executor;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallRequest;
import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.mcp.riskevaluate.RiskEvaluateQuery;
import com.flightpathfinder.mcp.riskevaluate.RiskEvaluateResult;
import com.flightpathfinder.mcp.riskevaluate.RiskEvaluateService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * {@code risk.evaluate} 的服务端执行器。
 *
 * <p>该工具保留在 MCP 服务端内执行，因为它要结合参考数据查询与本地规则评分。
 * 引导侧仅通过 MCP 消费结构化风险结果。
 */
@Component
public class RiskEvaluateMcpToolExecutor implements McpToolExecutor {

    private static final String TOOL_ID = "risk.evaluate";
    private static final Pattern AIRPORT_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern AIRLINE_PATTERN = Pattern.compile("^[A-Z0-9]{2}$");

    private final RiskEvaluateService riskEvaluateService;

    public RiskEvaluateMcpToolExecutor(RiskEvaluateService riskEvaluateService) {
        this.riskEvaluateService = riskEvaluateService;
    }

    /**
     * 描述中转风险评估的 MCP 契约。
     *
     * @return {@code risk.evaluate} 的工具描述
     */
    @Override
    public McpToolDescriptor descriptor() {
        return new McpToolDescriptor(
                TOOL_ID,
                "Risk Evaluate",
                "Evaluates transfer risk by combining hub efficiency, airline punctuality class and available connection buffer.",
                Map.of(
                        "type", "object",
                        "required", List.of("hubAirport", "firstAirline", "secondAirline", "bufferHours"),
                        "properties", Map.of(
                                "hubAirport", Map.of("type", "string"),
                                "firstAirline", Map.of("type", "string"),
                                "secondAirline", Map.of("type", "string"),
                                "bufferHours", Map.of("type", "number")
                        )),
                Map.of(
                        "type", "object",
                        "properties", Map.ofEntries(
                                Map.entry("status", Map.of("type", "string")),
                                Map.entry("message", Map.of("type", "string")),
                                Map.entry("retryable", Map.of("type", "boolean")),
                                Map.entry("suggestedAction", Map.of("type", "string")),
                                Map.entry("hubAirport", Map.of("type", "string")),
                                Map.entry("firstAirline", Map.of("type", "string")),
                                Map.entry("secondAirline", Map.of("type", "string")),
                                Map.entry("bufferHours", Map.of("type", "number")),
                                Map.entry("riskLevel", Map.of("type", "string")),
                                Map.entry("riskScore", Map.of("type", "number")),
                                Map.entry("airlineRiskScore", Map.of("type", "number")),
                                Map.entry("bufferRiskScore", Map.of("type", "number")),
                                Map.entry("hubRiskScore", Map.of("type", "number")),
                                Map.entry("suggestedBufferHours", Map.of("type", "number")),
                                Map.entry("explanation", Map.of("type", "string")),
                                Map.entry("recommendations", Map.of("type", "array")),
                                Map.entry("sameAirline", Map.of("type", "boolean")),
                                Map.entry("dataComplete", Map.of("type", "boolean")),
                                Map.entry("missingInputs", Map.of("type", "array"))
                        )));
    }

    /**
     * 执行中转风险规则流水线。
     *
     * @param request MCP 请求，包含枢纽机场、航司与中转缓冲时长输入
     * @return 结构化风险结果，业务状态可能为 {@code LOW}、{@code MEDIUM}、
     *     {@code HIGH} 或 {@code DATA_NOT_FOUND}
     */
    @Override
    public McpToolCallResult execute(McpToolCallRequest request) {
        try {
            RiskEvaluateQuery query = parseRequest(request.arguments());
            RiskEvaluateResult result = riskEvaluateService.evaluate(query);
            String status = result.riskLevel();
            String message = buildMessage(result);
            return new McpToolCallResult(
                    TOOL_ID,
                    true,
                    message,
                    successBody(result, status, message),
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
                            "message", "risk evaluation failed unexpectedly",
                            "retryable", false,
                            "suggestedAction", "NONE",
                            "dataComplete", false,
                            "missingInputs", List.of(),
                            "recommendations", List.of()),
                    exception.getMessage());
        }
    }

    private RiskEvaluateQuery parseRequest(Map<String, Object> arguments) {
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String hubAirport = stringValue(safeArguments.get("hubAirport"));
        String firstAirline = stringValue(safeArguments.get("firstAirline"));
        String secondAirline = stringValue(safeArguments.get("secondAirline"));
        Double bufferHours = doubleValue(safeArguments.get("bufferHours"));
        if (hubAirport == null || firstAirline == null || secondAirline == null || bufferHours == null) {
            throw new IllegalArgumentException("hubAirport, firstAirline, secondAirline and bufferHours are required");
        }

        String normalizedHub = hubAirport.toUpperCase(Locale.ROOT);
        String normalizedFirstAirline = firstAirline.toUpperCase(Locale.ROOT);
        String normalizedSecondAirline = secondAirline.toUpperCase(Locale.ROOT);
        if (!AIRPORT_PATTERN.matcher(normalizedHub).matches()) {
            throw new IllegalArgumentException("hubAirport must be a 3-letter airport code");
        }
        if (!AIRLINE_PATTERN.matcher(normalizedFirstAirline).matches()) {
            throw new IllegalArgumentException("firstAirline must be a 2-character airline code");
        }
        if (!AIRLINE_PATTERN.matcher(normalizedSecondAirline).matches()) {
            throw new IllegalArgumentException("secondAirline must be a 2-character airline code");
        }
        if (bufferHours <= 0D) {
            throw new IllegalArgumentException("bufferHours must be greater than 0");
        }
        // 在执行器入口保持严格校验，让风险引擎专注评分本身，
        // 而不是防御传输层原始输入形态。
        return new RiskEvaluateQuery(normalizedHub, normalizedFirstAirline, normalizedSecondAirline, bufferHours);
    }

    private String buildMessage(RiskEvaluateResult result) {
        return switch (result.riskLevel()) {
            case "DATA_NOT_FOUND" -> "risk evaluation data is not complete enough for the current transfer scenario";
            case "LOW" -> "transfer risk is low";
            case "MEDIUM" -> "transfer risk is medium";
            case "HIGH" -> "transfer risk is high";
            default -> "risk evaluation completed";
        };
    }

    private Map<String, Object> successBody(RiskEvaluateResult result, String status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("message", message);
        body.put("retryable", false);
        body.put("suggestedAction", suggestedAction(status));
        body.put("hubAirport", result.hubAirport());
        body.put("firstAirline", result.firstAirline());
        body.put("secondAirline", result.secondAirline());
        body.put("bufferHours", result.bufferHours());
        body.put("riskLevel", result.riskLevel());
        body.put("riskScore", result.riskScore());
        body.put("airlineRiskScore", result.airlineRiskScore());
        body.put("bufferRiskScore", result.bufferRiskScore());
        body.put("hubRiskScore", result.hubRiskScore());
        body.put("suggestedBufferHours", result.suggestedBufferHours());
        body.put("explanation", result.explanation());
        body.put("recommendations", result.recommendations());
        body.put("sameAirline", result.sameAirline());
        body.put("dataComplete", result.dataComplete());
        body.put("missingInputs", result.missingInputs());
        return Map.copyOf(body);
    }

    private String suggestedAction(String status) {
        return switch (status) {
            case "DATA_NOT_FOUND" -> "PROVIDE_COMPLETE_INPUT";
            case "MEDIUM", "HIGH" -> "REVIEW_CONNECTION";
            default -> "NONE";
        };
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
                        "dataComplete", false,
                        "missingInputs", List.of(),
                        "recommendations", List.of()),
                message);
    }

    private String stringValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = String.valueOf(rawValue).trim();
        return value.isEmpty() ? null : value;
    }

    private Double doubleValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(rawValue));
    }
}
