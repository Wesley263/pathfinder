package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.framework.protocol.mcp.McpToolCallResult;
import com.flightpathfinder.rag.core.retrieve.KbContext;
import com.flightpathfinder.rag.core.retrieve.KbRetrievalItem;
import com.flightpathfinder.rag.core.retrieve.McpContext;
import com.flightpathfinder.rag.core.retrieve.McpExecutionRecord;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 最终回答阶段的默认输入装配器。
 *
 * 负责把 retrieval 结果转换为回答文本生成可直接消费的标准输入。
 * 同时提取证据摘要并标记 partial、snapshotMiss、empty 等状态。
 */
@Service
public class DefaultFinalAnswerAssembler implements FinalAnswerAssembler {

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
        * 组装最终回答提示输入。
     *
        * @param retrievalResult 检索阶段结果
     * @return 文本生成器可直接消费的标准输入
     */
    @Override
    public FinalAnswerPromptInput assemble(RetrievalResult retrievalResult) {
        RetrievalResult safeRetrievalResult = retrievalResult == null
                ? new RetrievalResult(null, null, null, null, null)
                : retrievalResult;
        StageOneRagResult stageOneResult = safeRetrievalResult.stageOneResult();
        KbContext kbContext = safeRetrievalResult.kbContext();
        McpContext mcpContext = safeRetrievalResult.mcpContext();
        boolean kbRequested = !stageOneResult.intentSplitResult().kbIntents().isEmpty();
        boolean mcpRequested = !stageOneResult.intentSplitResult().mcpIntents().isEmpty();

        List<AnswerEvidenceSummary> evidenceSummaries = new ArrayList<>();
        // 证据数量控制在前几条，避免提示词长度失控。
        kbContext.items().stream()
                .limit(4)
                .map(this::toKbEvidence)
                .forEach(evidenceSummaries::add);
        mcpContext.executions().stream()
                .limit(4)
                .map(this::toMcpEvidence)
                .forEach(evidenceSummaries::add);

        boolean hasKbAnswerMaterial = !kbContext.empty();
        boolean hasMcpAnswerMaterial = mcpContext.executions().stream()
                .anyMatch(execution -> switch (execution.status()) {
                    case "SUCCESS", "PARTIAL_SUCCESS", "NO_PATH_FOUND", "NO_FLIGHTS_FOUND", "NO_PRICE_FOUND", "DATA_NOT_FOUND", "LOW", "MEDIUM", "HIGH" -> true;
                    default -> false;
                });
        boolean partial = "PARTIAL_FAILURE".equals(safeRetrievalResult.status())
                || "PARTIAL_SUCCESS".equals(safeRetrievalResult.status())
                || (kbRequested && mcpRequested
                && (hasKbAnswerMaterial != hasMcpAnswerMaterial || safeRetrievalResult.hasSnapshotMiss()))
                || (kbRequested && !hasKbAnswerMaterial && hasMcpAnswerMaterial)
                || (mcpRequested && !hasMcpAnswerMaterial && hasKbAnswerMaterial)
                || mcpContext.executions().stream().anyMatch(execution -> "PARTIAL_SUCCESS".equals(execution.status()));

        return new FinalAnswerPromptInput(
                stageOneResult.rewriteResult().rewrittenQuestion(),
                stageOneResult.intentSplitResult(),
                kbContext,
                mcpContext,
                stageOneResult.memoryContext(),
                evidenceSummaries,
                partial,
                safeRetrievalResult.hasSnapshotMiss(),
                !hasKbAnswerMaterial && !hasMcpAnswerMaterial);
    }

    /**
        * 转换 KB 命中条目为证据摘要。
     *
        * @param item KB 命中条目
     * @return 证据摘要
     */
    private AnswerEvidenceSummary toKbEvidence(KbRetrievalItem item) {
        return new AnswerEvidenceSummary(
                "KB",
                item.source(),
                item.title(),
                "SUCCESS",
                abbreviate(item.content(), 180));
    }

    /**
        * 转换 MCP 执行记录为证据摘要。
     *
        * @param execution MCP 执行记录
     * @return 证据摘要
     */
    private AnswerEvidenceSummary toMcpEvidence(McpExecutionRecord execution) {
        String snippet = switch (execution.status()) {
            case "SUCCESS" -> summarizeSuccessfulMcp(execution.toolId(), execution.toolResult());
            case "PARTIAL_SUCCESS" -> summarizeSuccessfulMcp(execution.toolId(), execution.toolResult());
            case "LOW", "MEDIUM", "HIGH" -> summarizeSuccessfulMcp(execution.toolId(), execution.toolResult());
            case "SNAPSHOT_MISS" -> firstNonBlank(execution.message(),
                    "Graph snapshot is not ready yet; retry later or trigger rebuild.");
            case "NO_PATH_FOUND" -> firstNonBlank(execution.message(),
                    "No feasible path was found under the current constraints.");
            case "NO_FLIGHTS_FOUND" -> firstNonBlank(execution.message(),
                    "No direct flight options were found for the current query window.");
            case "NO_PRICE_FOUND" -> firstNonBlank(execution.message(),
                    "No price comparison result was found for the requested city pairs.");
            case "DATA_NOT_FOUND" -> summarizeDataNotFound(execution);
            default -> firstNonBlank(execution.error(), execution.message(), "MCP execution returned no usable payload.");
        };
        return new AnswerEvidenceSummary(
                "MCP",
                execution.toolId(),
                execution.intentName().isBlank() ? execution.toolId() : execution.intentName(),
                execution.status(),
                snippet);
    }

    /**
        * 汇总 MCP 成功执行结果。
     *
     * @param toolId 工具标识
     * @param toolResult 工具返回结果
     * @return 面向回答拼装的摘要文本
     */
    private String summarizeSuccessfulMcp(String toolId, McpToolCallResult toolResult) {
        return switch (toolId) {
            case GRAPH_PATH_TOOL_ID -> summarizeSuccessfulPath(toolResult);
            case FLIGHT_SEARCH_TOOL_ID -> summarizeSuccessfulFlightSearch(toolResult);
            case PRICE_LOOKUP_TOOL_ID -> summarizeSuccessfulPriceLookup(toolResult);
            case VISA_CHECK_TOOL_ID -> summarizeSuccessfulVisaCheck(toolResult);
            case CITY_COST_TOOL_ID -> summarizeSuccessfulCityCost(toolResult);
            case RISK_EVALUATE_TOOL_ID -> summarizeSuccessfulRiskEvaluate(toolResult);
            default -> firstNonBlank(toolResult == null ? "" : toolResult.content(), "MCP tool completed successfully.");
        };
    }

    /**
        * 汇总 MCP 数据缺失状态。
     *
        * @param execution MCP 执行记录
     * @return 状态说明文本
     */
    private String summarizeDataNotFound(McpExecutionRecord execution) {
        return switch (execution.toolId()) {
            case VISA_CHECK_TOOL_ID -> firstNonBlank(execution.message(),
                    "Visa policy data is not available for the requested countries.");
            case CITY_COST_TOOL_ID -> firstNonBlank(execution.message(),
                    "City cost data is not available for the requested cities.");
            case RISK_EVALUATE_TOOL_ID -> firstNonBlank(execution.message(),
                    "Risk evaluation data is not complete enough for the current transfer scenario.");
            default -> firstNonBlank(execution.message(),
                    "The MCP tool could not find matching business data for the current request.");
        };
    }

    /**
     * 汇总路径规划工具的成功结果。
     *
     * @param toolResult 工具返回结果
     * @return 路径摘要文本
     */
    @SuppressWarnings("unchecked")
    private String summarizeSuccessfulPath(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Graph path tool completed, but no structured result was returned.";
        }
        Object paths = toolResult.structuredContent().get("paths");
        if (!(paths instanceof List<?> pathList) || pathList.isEmpty()) {
            return firstNonBlank(toolResult.content(), "Graph path tool completed without candidate paths.");
        }
        Object firstPath = pathList.getFirst();
        if (!(firstPath instanceof Map<?, ?> pathMap)) {
            return firstNonBlank(toolResult.content(), "Graph path tool returned a non-standard candidate path payload.");
        }

        List<String> route = new ArrayList<>();
        Object legs = pathMap.get("legs");
        if (legs instanceof List<?> legList) {
            for (Object legObject : legList) {
                if (legObject instanceof Map<?, ?> legMap) {
                    String origin = stringValue(legMap.get("origin"));
                    String destination = stringValue(legMap.get("destination"));
                    if (!origin.isBlank() && route.isEmpty()) {
                        route.add(origin);
                    }
                    if (!destination.isBlank()) {
                        route.add(destination);
                    }
                }
            }
        }

        return "Best path "
                + (route.isEmpty() ? "is available" : String.join(" -> ", route))
                + ", totalPriceCny=" + stringValue(pathMap.get("totalPriceCny"))
                + ", totalDurationMinutes=" + stringValue(pathMap.get("totalDurationMinutes"));
    }

    /**
     * 汇总航班搜索工具的成功结果。
     *
     * @param toolResult 工具返回结果
     * @return 航班摘要文本
     */
    @SuppressWarnings("unchecked")
    private String summarizeSuccessfulFlightSearch(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Flight search completed, but no structured result was returned.";
        }
        Object flights = toolResult.structuredContent().get("flights");
        if (!(flights instanceof List<?> flightList) || flightList.isEmpty()) {
            return firstNonBlank(toolResult.content(), "Flight search completed without flight options.");
        }
        Object firstFlight = flightList.getFirst();
        if (!(firstFlight instanceof Map<?, ?> flightMap)) {
            return firstNonBlank(toolResult.content(), "Flight search returned a non-standard flight payload.");
        }

        return "Best flight "
                + stringValue(flightMap.get("airlineCode"))
                + " "
                + stringValue(flightMap.get("origin"))
                + " -> "
                + stringValue(flightMap.get("destination"))
                + " on "
                + stringValue(flightMap.get("date"))
                + ", priceCny="
                + stringValue(flightMap.get("priceCny"))
                + ", durationMinutes="
                + stringValue(flightMap.get("durationMinutes"));
    }

    /**
     * 汇总价格比较工具的成功结果。
     *
     * @param toolResult 工具返回结果
     * @return 价格比较摘要文本
     */
    @SuppressWarnings("unchecked")
    private String summarizeSuccessfulPriceLookup(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Price lookup completed, but no structured result was returned.";
        }
        Object prices = toolResult.structuredContent().get("prices");
        if (!(prices instanceof List<?> priceList) || priceList.isEmpty()) {
            return firstNonBlank(toolResult.content(), "Price lookup completed without comparison entries.");
        }
        Object firstPrice = priceList.getFirst();
        if (!(firstPrice instanceof Map<?, ?> priceMap)) {
            return firstNonBlank(toolResult.content(), "Price lookup returned a non-standard comparison payload.");
        }

        String summary = "Lowest fare for "
                + stringValue(priceMap.get("cityPair"))
                + " is "
                + stringValue(priceMap.get("lowestPriceCny"))
                + " CNY";
        Object missingPairs = toolResult.structuredContent().get("missingPairs");
        if (missingPairs instanceof List<?> missingList && !missingList.isEmpty()) {
            summary += ", missingPairs=" + String.join(", ", missingList.stream().map(String::valueOf).toList());
        }
        return summary;
    }

    /**
     * 汇总签证检查工具的成功结果。
     *
     * @param toolResult 工具返回结果
     * @return 签证结果摘要文本
     */
    @SuppressWarnings("unchecked")
    private String summarizeSuccessfulVisaCheck(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Visa check completed, but no structured result was returned.";
        }
        Object results = toolResult.structuredContent().get("results");
        if (!(results instanceof List<?> resultList) || resultList.isEmpty()) {
            return firstNonBlank(toolResult.content(), "Visa check completed without country results.");
        }
        List<String> summaries = new ArrayList<>();
        for (int index = 0; index < Math.min(3, resultList.size()); index++) {
            Object resultObject = resultList.get(index);
            if (resultObject instanceof Map<?, ?> resultMap) {
                summaries.add(stringValue(resultMap.get("countryCode")) + "=" + stringValue(resultMap.get("visaStatus")));
            }
        }
        return "Visa status under passport "
                + stringValue(toolResult.structuredContent().get("passportCountry"))
                + ": "
                + String.join(", ", summaries);
    }

    /**
     * 汇总城市成本工具的成功结果。
     *
     * @param toolResult 工具返回结果
     * @return 城市成本摘要文本
     */
    @SuppressWarnings("unchecked")
    private String summarizeSuccessfulCityCost(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "City cost lookup completed, but no structured result was returned.";
        }
        Object costs = toolResult.structuredContent().get("costs");
        if (!(costs instanceof List<?> costList) || costList.isEmpty()) {
            return firstNonBlank(toolResult.content(), "City cost lookup completed without city entries.");
        }
        List<String> summaries = new ArrayList<>();
        for (int index = 0; index < Math.min(3, costList.size()); index++) {
            Object costObject = costList.get(index);
            if (costObject instanceof Map<?, ?> costMap) {
                summaries.add(stringValue(costMap.get("iataCode"))
                        + "="
                        + stringValue(costMap.get("dailyCostUsd"))
                        + " USD/day");
            }
        }
        String summary = "City cost comparison: " + String.join(", ", summaries);
        Object missingCities = toolResult.structuredContent().get("missingCities");
        if (missingCities instanceof List<?> missingList && !missingList.isEmpty()) {
            summary += ", missingCities=" + String.join(", ", missingList.stream().map(String::valueOf).toList());
        }
        return summary;
    }

    /**
     * 汇总风险评估工具的成功结果。
     *
     * @param toolResult 工具返回结果
     * @return 风险评估摘要文本
     */
    private String summarizeSuccessfulRiskEvaluate(McpToolCallResult toolResult) {
        if (toolResult == null || toolResult.structuredContent() == null) {
            return "Risk evaluation completed, but no structured result was returned.";
        }
        return "Transfer risk at "
                + stringValue(toolResult.structuredContent().get("hubAirport"))
                + " is "
                + firstNonBlank(
                stringValue(toolResult.structuredContent().get("riskLevel")),
                stringValue(toolResult.structuredContent().get("status")))
                + ", score="
                + stringValue(toolResult.structuredContent().get("riskScore"))
                + ", suggestedBufferHours="
                + stringValue(toolResult.structuredContent().get("suggestedBufferHours"));
    }

    /**
     * 对长文本做安全截断。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
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

