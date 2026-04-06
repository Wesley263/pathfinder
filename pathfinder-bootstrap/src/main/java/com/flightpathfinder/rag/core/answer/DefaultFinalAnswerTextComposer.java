package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.rag.core.retrieve.KbRetrievalItem;
import com.flightpathfinder.rag.core.retrieve.McpExecutionRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
@Service
public class DefaultFinalAnswerTextComposer implements FinalAnswerTextComposer {

    /** 路径规划工具标识。 */
    private static final String GRAPH_PATH_TOOL_ID = "graph.path.search";
    /** 航班搜索工具标识。 */
    private static final String FLIGHT_SEARCH_TOOL_ID = "flight.search";
    /** 价格比较工具标识。 */
    private static final String PRICE_LOOKUP_TOOL_ID = "price.lookup";
    /** 签证检查工具标识。 */
    private static final String VISA_CHECK_TOOL_ID = "visa.check";
    /** 城市成本工具标识。 */
    private static final String CITY_COST_TOOL_ID = "city.cost";
    /** 风险评估工具标识。 */
    private static final String RISK_EVALUATE_TOOL_ID = "risk.evaluate";

    /**
     * 基于标准输入生成最终回答文本。
     *
     * @param promptInput 回答生成输入
     * @return 面向调用方的最终回答文本
     */
    @Override
    public String compose(FinalAnswerPromptInput promptInput) {
        if (promptInput == null || promptInput.empty()) {
            return "No usable KB or MCP context is available for the current question yet.";
        }

        List<String> sections = new ArrayList<>();
        if (!promptInput.rewrittenQuestion().isBlank()) {
            sections.add("Question: " + promptInput.rewrittenQuestion());
        }

        // 说明。
        String mcpSection = composeMcpSection(promptInput);
        if (!mcpSection.isBlank()) {
            sections.add(mcpSection);
        }

        String kbSection = composeKbSection(promptInput);
        if (!kbSection.isBlank()) {
            sections.add(kbSection);
        }

        if (promptInput.snapshotMissAffected()) {
            sections.add("Graph snapshot status: the MCP side reported SNAPSHOT_MISS, so path planning context is not fully ready. Retry later or trigger rebuild if you need the graph/path result.");
        } else if (promptInput.partial()) {
            sections.add("Answer scope: this answer is partial and only uses the currently available retrieval context.");
        }

        return String.join("\n\n", sections);
    }

    /**
     * 说明。
     *
     * @param promptInput 回答生成输入
     * @return 返回结果。
     */
    private String composeMcpSection(FinalAnswerPromptInput promptInput) {
        List<McpExecutionRecord> executions = promptInput.mcpContext().executions();
        if (executions.isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        for (McpExecutionRecord execution : executions) {
            switch (execution.status()) {
                case "SUCCESS" -> lines.add(composeSuccessfulMcpLine(execution.toolId(), execution.toolResult()));
                case "PARTIAL_SUCCESS" -> lines.add(composeSuccessfulMcpLine(execution.toolId(), execution.toolResult()));
                case "LOW", "MEDIUM", "HIGH" -> lines.add(composeSuccessfulMcpLine(execution.toolId(), execution.toolResult()));
                case "NO_PATH_FOUND" ->
                        lines.add("Path planning result: no feasible path was found under the current constraints.");
                case "NO_FLIGHTS_FOUND" ->
                        lines.add("Flight search result: no direct flight options were found for the current query window.");
                case "NO_PRICE_FOUND" ->
                        lines.add("Price lookup result: no price comparison result was found for the requested city pairs.");
                case "DATA_NOT_FOUND" ->
                        lines.add(composeDataNotFoundLine(execution));
                case "SNAPSHOT_MISS" ->
                        lines.add("Path planning result: graph snapshot is not ready yet, so the path tool could not produce route candidates.");
                case "MISSING_REQUIRED", "INVALID_PARAMETER" ->
                        lines.add(friendlyToolName(execution.toolId()) + " could not run because required parameters were incomplete or invalid: "
                                + firstNonBlank(execution.error(), "check the required MCP parameters."));
                default -> lines.add(friendlyToolName(execution.toolId()) + " status " + execution.status() + ": "
                        + firstNonBlank(execution.error(), execution.message(), "no additional detail was returned."));
            }
        }

        if (lines.isEmpty()) {
            return "";
        }
        return "MCP context:\n- " + String.join("\n- ", lines);
    }

    /**
     * 说明。
     *
     * @param toolId 工具标识
     * @param toolResult 工具结果
     * @return 单行说明文本
     */
    private String composeSuccessfulMcpLine(String toolId, McpToolCallResult toolResult) {
        return switch (toolId) {
            case GRAPH_PATH_TOOL_ID -> composeSuccessfulPathLine(toolResult);
            case FLIGHT_SEARCH_TOOL_ID -> composeSuccessfulFlightSearchLine(toolResult);
            case PRICE_LOOKUP_TOOL_ID -> composeSuccessfulPriceLookupLine(toolResult);
            case VISA_CHECK_TOOL_ID -> composeSuccessfulVisaCheckLine(toolResult);
            case CITY_COST_TOOL_ID -> composeSuccessfulCityCostLine(toolResult);
            case RISK_EVALUATE_TOOL_ID -> composeSuccessfulRiskEvaluateLine(toolResult);
            default -> firstNonBlank(toolResult == null ? "" : toolResult.content(), "MCP tool completed successfully.");
        };
    }

    /**
     * 说明。
     *
     * @param execution 参数说明。
     * @return 用户可读说明
     */
    private String composeDataNotFoundLine(McpExecutionRecord execution) {
        return switch (execution.toolId()) {
            case VISA_CHECK_TOOL_ID -> "Visa check result: visa policy data is not available for the requested countries.";
            case CITY_COST_TOOL_ID -> "City cost result: city cost data is not available for the requested cities.";
            case RISK_EVALUATE_TOOL_ID -> "Risk evaluation result: the MCP side does not have enough airline or hub data to complete this assessment.";
            default -> friendlyToolName(execution.toolId()) + " result: no matching business data is available for the current request.";
        };
    }

    /**
     * 生成路径规划成功态说明。
     *
     * @param toolResult 工具结果
     * @return 单行说明
     */
    @SuppressWarnings("unchecked")
    private String composeSuccessfulPathLine(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Path planning completed, but no structured route payload was returned.";
        }

        Object paths = toolResult.structuredContent().get("paths");
        if (!(paths instanceof List<?> pathList) || pathList.isEmpty()) {
            return "Path planning completed, but the route list is empty.";
        }
        Object firstPath = pathList.getFirst();
        if (!(firstPath instanceof Map<?, ?> pathMap)) {
            return "Path planning completed, but the route payload is not in the expected shape.";
        }

        String route = buildRoute(pathMap.get("legs"));
        return "Best route: " + route
                + "; totalPriceCny=" + stringValue(pathMap.get("totalPriceCny"))
                + "; totalDurationMinutes=" + stringValue(pathMap.get("totalDurationMinutes"))
                + "; transferCount=" + stringValue(pathMap.get("transferCount"));
    }

    /**
     * 生成航班搜索成功态说明。
     *
     * @param toolResult 工具结果
     * @return 单行说明
     */
    @SuppressWarnings("unchecked")
    private String composeSuccessfulFlightSearchLine(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Flight search completed, but no structured flight payload was returned.";
        }

        Object flights = toolResult.structuredContent().get("flights");
        if (!(flights instanceof List<?> flightList) || flightList.isEmpty()) {
            return "Flight search completed, but the flight list is empty.";
        }
        Object firstFlight = flightList.getFirst();
        if (!(firstFlight instanceof Map<?, ?> flightMap)) {
            return "Flight search completed, but the flight payload is not in the expected shape.";
        }

        return "Best flight: "
                + stringValue(flightMap.get("airlineCode"))
                + " "
                + stringValue(flightMap.get("origin"))
                + " -> "
                + stringValue(flightMap.get("destination"))
                + " on "
                + stringValue(flightMap.get("date"))
                + "; priceCny="
                + stringValue(flightMap.get("priceCny"))
                + "; durationMinutes="
                + stringValue(flightMap.get("durationMinutes"))
                + "; optionCount="
                + stringValue(toolResult.structuredContent().get("flightCount"));
    }

    /**
     * 生成价格比较成功态说明。
     *
     * @param toolResult 工具结果
     * @return 单行说明
     */
    @SuppressWarnings("unchecked")
    private String composeSuccessfulPriceLookupLine(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Price lookup completed, but no structured comparison payload was returned.";
        }

        Object prices = toolResult.structuredContent().get("prices");
        if (!(prices instanceof List<?> priceList) || priceList.isEmpty()) {
            return "Price lookup completed, but the comparison list is empty.";
        }
        List<String> entries = new ArrayList<>();
        for (int index = 0; index < Math.min(3, priceList.size()); index++) {
            Object priceObject = priceList.get(index);
            if (priceObject instanceof Map<?, ?> priceMap) {
                entries.add(stringValue(priceMap.get("cityPair"))
                        + "="
                        + stringValue(priceMap.get("lowestPriceCny"))
                        + " CNY");
            }
        }
        String line = "Price comparison: " + String.join(", ", entries);
        Object missingPairs = toolResult.structuredContent().get("missingPairs");
        if (missingPairs instanceof List<?> missingList && !missingList.isEmpty()) {
            line += "; missingPairs=" + String.join(", ", missingList.stream().map(String::valueOf).toList());
        }
        return line;
    }

    /**
     * 生成签证检查成功态说明。
     *
     * @param toolResult 工具结果
     * @return 单行说明
     */
    @SuppressWarnings("unchecked")
    private String composeSuccessfulVisaCheckLine(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Visa check completed, but no structured visa payload was returned.";
        }

        Object results = toolResult.structuredContent().get("results");
        if (!(results instanceof List<?> resultList) || resultList.isEmpty()) {
            return "Visa check completed, but the result list is empty.";
        }
        List<String> entries = new ArrayList<>();
        for (int index = 0; index < Math.min(3, resultList.size()); index++) {
            Object resultObject = resultList.get(index);
            if (resultObject instanceof Map<?, ?> resultMap) {
                entries.add(stringValue(resultMap.get("countryCode"))
                        + "="
                        + stringValue(resultMap.get("visaStatus")));
            }
        }
        return "Visa check under passport "
                + stringValue(toolResult.structuredContent().get("passportCountry"))
                + ": "
                + String.join(", ", entries);
    }

    /**
     * 生成城市成本成功态说明。
     *
     * @param toolResult 工具结果
     * @return 单行说明
     */
    @SuppressWarnings("unchecked")
    private String composeSuccessfulCityCostLine(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "City cost lookup completed, but no structured city cost payload was returned.";
        }

        Object costs = toolResult.structuredContent().get("costs");
        if (!(costs instanceof List<?> costList) || costList.isEmpty()) {
            return "City cost lookup completed, but the city cost list is empty.";
        }
        List<String> entries = new ArrayList<>();
        for (int index = 0; index < Math.min(3, costList.size()); index++) {
            Object costObject = costList.get(index);
            if (costObject instanceof Map<?, ?> costMap) {
                entries.add(stringValue(costMap.get("iataCode"))
                        + "="
                        + stringValue(costMap.get("dailyCostUsd"))
                        + " USD/day");
            }
        }
        String line = "City cost comparison: " + String.join(", ", entries);
        Object missingCities = toolResult.structuredContent().get("missingCities");
        if (missingCities instanceof List<?> missingList && !missingList.isEmpty()) {
            line += "; missingCities=" + String.join(", ", missingList.stream().map(String::valueOf).toList());
        }
        return line;
    }

    /**
     * 生成风险评估成功态说明。
     *
     * @param toolResult 工具结果
     * @return 单行说明
     */
    private String composeSuccessfulRiskEvaluateLine(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Risk evaluation completed, but no structured risk payload was returned.";
        }
        return "Transfer risk: "
                + firstNonBlank(
                stringValue(toolResult.structuredContent().get("riskLevel")),
                stringValue(toolResult.structuredContent().get("status")))
                + " at "
                + stringValue(toolResult.structuredContent().get("hubAirport"))
                + " for "
                + stringValue(toolResult.structuredContent().get("firstAirline"))
                + " -> "
                + stringValue(toolResult.structuredContent().get("secondAirline"))
                + "; bufferHours="
                + stringValue(toolResult.structuredContent().get("bufferHours"))
                + "; suggestedBufferHours="
                + stringValue(toolResult.structuredContent().get("suggestedBufferHours"))
                + "; explanation="
                + stringValue(toolResult.structuredContent().get("explanation"));
    }

    /**
     * 说明。
     *
     * @param legsObject 参数说明。
     * @return 返回结果。
     */
    private String buildRoute(Object legsObject) {
        if (!(legsObject instanceof List<?> legs) || legs.isEmpty()) {
            return "route unavailable";
        }

        List<String> nodes = new ArrayList<>();
        for (Object legObject : legs) {
            if (legObject instanceof Map<?, ?> legMap) {
                String origin = stringValue(legMap.get("origin"));
                String destination = stringValue(legMap.get("destination"));
                if (!origin.isBlank() && nodes.isEmpty()) {
                    nodes.add(origin);
                }
                if (!destination.isBlank()) {
                    nodes.add(destination);
                }
            }
        }
        return nodes.isEmpty() ? "route unavailable" : String.join(" -> ", nodes);
    }

    /**
     * 说明。
     *
     * @param promptInput 回答生成输入
     * @return 返回结果。
     */
    private String composeKbSection(FinalAnswerPromptInput promptInput) {
        if (promptInput.kbContext().empty()) {
            return "";
        }

        List<KbRetrievalItem> items = promptInput.kbContext().items();
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < Math.min(3, items.size()); index++) {
            KbRetrievalItem item = items.get(index);
            lines.add(item.title() + " [" + item.collectionName() + "]: " + abbreviate(item.content(), 180));
        }
        return lines.isEmpty() ? "" : "KB context:\n- " + String.join("\n- ", lines);
    }

    /**
     * 返回工具友好名称。
     *
     * @param toolId 工具标识
     * @return 友好名称
     */
    private String friendlyToolName(String toolId) {
        return switch (toolId) {
            case GRAPH_PATH_TOOL_ID -> "Path planning";
            case FLIGHT_SEARCH_TOOL_ID -> "Flight search";
            case PRICE_LOOKUP_TOOL_ID -> "Price lookup";
            case VISA_CHECK_TOOL_ID -> "Visa check";
            case CITY_COST_TOOL_ID -> "City cost";
            case RISK_EVALUATE_TOOL_ID -> "Risk evaluation";
            default -> toolId == null || toolId.isBlank() ? "MCP tool" : toolId;
        };
    }

    /**
     * 对长文本做安全截断。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后文本
     */
    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    /**
     * 把任意对象转换为去空白字符串。
     *
     * @param value 原始值
     * @return 字符串值
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 返回首个非空白文本。
     *
     * @param values 候选文本
     * @return 首个非空白值；若都为空则返回空字符串
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
