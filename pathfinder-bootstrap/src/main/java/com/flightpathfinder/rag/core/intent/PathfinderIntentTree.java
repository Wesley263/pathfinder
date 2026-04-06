package com.flightpathfinder.rag.core.intent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 当前 Pathfinder 2.0 启用的静态意图树。
 *
 * <p>它定义第一阶段已接入的 MCP、KB 和 SYSTEM 三类叶子节点。之所以仍然保留在 bootstrap/rag 内，
 * 是因为这棵树描述的是当前主链的业务分流事实，而不是通用框架能力。</p>
 */
@Component
public class PathfinderIntentTree implements IntentTree {

    /** 当前意图树的根路径前缀。 */
    private static final String ROOT_PATH = "flight-travel";

    /** 整棵意图树的根节点。 */
    private final IntentNode root;
    /** 所有可直接参与分类的叶子节点。 */
    private final List<IntentNode> leafNodes;
    /** 叶子节点索引，支持按 id、别名和 toolId 快速查找。 */
    private final Map<String, IntentNode> leafIndex;

    /**
     * 构造默认意图树。
     *
     * <p>当前使用静态树而不是可配置树，是因为第一阶段先要把主链边界稳定下来，再考虑配置化演进。</p>
     */
    public PathfinderIntentTree() {
        IntentNode mcpCategory = IntentNode.category(
                "structured_query",
                "Structured Query",
                "Questions that should be routed to structured MCP tools.",
                ROOT_PATH + "/structured-query",
                List.of(
                        IntentNode.mcpTopic(
                                "path_optimize",
                                "Path Optimize",
                                "Plan graph/path routes with budget and transfer constraints.",
                                ROOT_PATH + "/structured-query/path-optimize",
                                "graph.path.search",
                                List.of("路径优化", "路径规划", "路线规划", "路线", "路径", "中转方案", "预算", "最便宜", "省钱", "怎么飞", "怎么走", "方案"),
                                List.of("graph.path.search", "graph_path_search"),
                                List.of("预算 4000 从 SHA 怎么飞到 LON", "帮我规划便宜的中转路线")),
                        IntentNode.mcpTopic(
                                "flight_search",
                                "Flight Search",
                                "Search direct flight options for a specific origin, destination and date window.",
                                ROOT_PATH + "/structured-query/flight-search",
                                "flight.search",
                                List.of("航班", "机票", "查航班", "搜航班", "航班搜索", "直飞", "票价", "出发日期", "航班信息", "flight search"),
                                List.of("flight.search", "flight_search"),
                                List.of("帮我查 2026-05-01 从 PVG 到 LHR 的航班", "搜索明天从 SHA 飞 NRT 的直飞航班")),
                        IntentNode.mcpTopic(
                                "price_lookup",
                                "Price Lookup",
                                "Compare lowest flight prices across multiple city pairs on the same date.",
                                ROOT_PATH + "/structured-query/price-lookup",
                                "price.lookup",
                                List.of("比价", "价格比较", "价格对比", "机票比较", "哪个便宜", "最低价", "对比价格", "比一比", "price lookup"),
                                List.of("price.lookup", "price_lookup"),
                                List.of("比较 PVG 到 NRT 和 PVG 到 ICN 哪个更便宜", "帮我对比 SHA,NRT;SHA,ICN 明天的价格")),
                        IntentNode.mcpTopic(
                                "visa_check",
                                "Visa Check",
                                "Check visa-free, transit-free, required or missing-data status for destination countries.",
                                ROOT_PATH + "/structured-query/visa-check",
                                "visa.check",
                                List.of("签证", "免签", "过境免签", "转机免签", "入境要求", "visa", "visa check"),
                                List.of("visa.check", "visa_check"),
                                List.of("日本需要签证吗", "帮我查 JP,KR 停留 3 天的签证要求")),
                        IntentNode.mcpTopic(
                                "city_cost",
                                "City Cost",
                                "Compare daily living costs across one or more cities using city-level cost data.",
                                ROOT_PATH + "/structured-query/city-cost",
                                "city.cost",
                                List.of("城市成本", "生活成本", "消费水平", "城市开销", "日均花费", "住宿成本", "餐饮成本", "交通成本", "cost of living", "city cost"),
                                List.of("city.cost", "city_cost"),
                                List.of("东京首尔曼谷哪个生活成本更低", "帮我查 NRT,ICN,BKK 的日均生活成本")),
                        IntentNode.mcpTopic(
                                "risk_evaluate",
                                "Risk Evaluate",
                                "Evaluate transfer risk based on hub efficiency, airlines and buffer time.",
                                ROOT_PATH + "/structured-query/risk-evaluate",
                                "risk.evaluate",
                                List.of("风险评估", "中转风险", "转机风险", "衔接风险", "赶得上吗", "稳不稳", "误点风险", "缓冲时间", "risk evaluate", "transfer risk"),
                                List.of("risk.evaluate", "risk_evaluate"),
                                List.of("NRT 中转 MU 接 NH 2 小时风险高吗", "帮我评估 ICN 转机 3 小时的衔接风险"))));

        IntentNode kbCategory = IntentNode.category(
                "knowledge",
                "Knowledge",
                "Questions that should go to KB retrieval instead of tool execution.",
                ROOT_PATH + "/knowledge",
                List.of(
                        IntentNode.kbTopic(
                                "policy_knowledge",
                                "Policy Knowledge",
                                "Visa, entry, transit and policy edge-case questions.",
                                ROOT_PATH + "/knowledge/policy-knowledge",
                                "policy_edge_cases",
                                4,
                                List.of("签证", "免签", "过境", "入境", "政策", "通关"),
                                List.of("visa_policy"),
                                List.of("去日本需要签证吗", "过境免签有什么限制")),
                        IntentNode.kbTopic(
                                "transfer_experience",
                                "Transfer Experience",
                                "Airport transfer process, terminal flow and real-world experience.",
                                ROOT_PATH + "/knowledge/transfer-experience",
                                "travel_experience",
                                5,
                                List.of("转机体验", "转机流程", "机场流程", "衔接", "值机", "过夜转机", "体验"),
                                List.of("travel_experience"),
                                List.of("成田机场转机方便吗", "首尔转机需要预留多久")),
                        IntentNode.kbTopic(
                                "travel_tips",
                                "Travel Tips",
                                "General travel guidance, luggage, timing and preparation tips.",
                                ROOT_PATH + "/knowledge/travel-tips",
                                "faq_and_tips",
                                5,
                                List.of("建议", "攻略", "注意事项", "行李", "提前多久", "避坑", "经验"),
                                List.of("faq_and_tips"),
                                List.of("国际航班提前多久到机场", "廉航行李怎么准备"))));

        IntentNode systemCategory = IntentNode.category(
                "general",
                "General",
                "General chat or out-of-scope requests.",
                ROOT_PATH + "/general",
                List.of(
                        IntentNode.systemTopic(
                                "general_assistant",
                                "General Assistant",
                                "Greeting, chitchat or requests outside the current flight domain.",
                                ROOT_PATH + "/general/general-assistant",
                                List.of("你好", "您好", "hello", "hi", "你是谁", "讲个笑话", "写首诗"),
                                List.of("chitchat", "general_chat"),
                                List.of("你好", "你是谁"))));

        this.root = IntentNode.domain(
                "travel",
                "Travel",
                "Pathfinder 2.0 stage-one intent tree.",
                ROOT_PATH,
                List.of(mcpCategory, kbCategory, systemCategory));
        this.leafNodes = root.children().stream()
                .flatMap(category -> category.children().stream())
                .toList();
        this.leafIndex = buildLeafIndex(leafNodes);
    }

    /**
     * 返回意图树根节点。
     *
     * @return 根节点
     */
    @Override
    public IntentNode root() {
        return root;
    }

    /**
     * 返回全部叶子节点。
     *
     * @return 可直接参与分类的叶子节点列表
     */
    @Override
    public List<IntentNode> leafNodes() {
        return leafNodes;
    }

    /**
     * 根据标识查找叶子节点。
     *
     * @param nodeId 节点 id、别名或 toolId
     * @return 匹配到的叶子节点
     */
    @Override
    public Optional<IntentNode> findLeafNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(leafIndex.get(normalize(nodeId)));
    }

    /**
     * 构建叶子节点索引。
     *
     * @param leafNodes 叶子节点列表
     * @return 供快速查找使用的不可变索引表
     */
    private static Map<String, IntentNode> buildLeafIndex(List<IntentNode> leafNodes) {
        Map<String, IntentNode> index = new LinkedHashMap<>();
        for (IntentNode leafNode : leafNodes) {
            index.put(normalize(leafNode.id()), leafNode);
            if (leafNode.mcpToolId() != null) {
                index.put(normalize(leafNode.mcpToolId()), leafNode);
            }
            leafNode.aliases().forEach(alias -> index.put(normalize(alias), leafNode));
        }
        return Map.copyOf(index);
    }

    /**
     * 统一节点索引键的比较格式。
     *
     * @param value 原始文本
     * @return 小写化后的索引键
     */
    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

