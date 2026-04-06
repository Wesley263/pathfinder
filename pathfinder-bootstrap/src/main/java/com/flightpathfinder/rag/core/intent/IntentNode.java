package com.flightpathfinder.rag.core.intent;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 意图树节点定义。
 *
 * 用于统一表达领域、分类与叶子主题节点，支持关键词匹配与执行元信息承载。
 *
 * @param id 节点唯一标识
 * @param name 节点名称
 * @param description 节点说明
 * @param level 节点所在层级
 * @param kind 叶子节点所属的大类；非叶子节点允许为空
 * @param keywords 主要关键词
 * @param examples 示例问题
 * @param aliases 额外别名或同义表达
 * @param fullPath 节点在意图树中的完整路径
 * @param children 子节点列表
 * @param mcpToolId MCP 叶子节点绑定的工具标识
 * @param collectionName 知识库叶子节点绑定的集合名
 * @param topK 知识库检索默认返回条数
 */
public record IntentNode(
        String id,
        String name,
        String description,
        IntentLevel level,
        IntentKind kind,
        List<String> keywords,
        List<String> examples,
        List<String> aliases,
        String fullPath,
        List<IntentNode> children,
        String mcpToolId,
        String collectionName,
        Integer topK) {

    /**
     * 校验并归一化意图节点。
     *
        * 对可选集合字段做不可变包装，并校验 Topic 节点必须声明类型。
     */
    public IntentNode {
        id = Objects.requireNonNull(id, "id cannot be null");
        name = Objects.requireNonNull(name, "name cannot be null");
        description = Objects.requireNonNull(description, "description cannot be null");
        level = Objects.requireNonNull(level, "level cannot be null");
        keywords = List.copyOf(keywords == null ? List.of() : keywords);
        examples = List.copyOf(examples == null ? List.of() : examples);
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        children = List.copyOf(children == null ? List.of() : children);
        if (level == IntentLevel.TOPIC && kind == null) {
            throw new IllegalArgumentException("topic intent node must declare kind");
        }
    }

    /**
     * 判断当前节点是否为叶子节点。
     *
     * @return 当节点不存在子节点时返回 true
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * 判断给定标识是否能匹配当前节点。
     *
        * @param candidateId 待匹配的节点标识或工具别名
        * @return 匹配 id、mcpToolId 或 aliases 时返回 true
     */
    public boolean matchesId(String candidateId) {
        if (candidateId == null || candidateId.isBlank()) {
            return false;
        }
        String normalized = normalize(candidateId);
        if (normalize(id).equals(normalized)) {
            return true;
        }
        if (mcpToolId != null && normalize(mcpToolId).equals(normalized)) {
            return true;
        }
        return aliases.stream().map(IntentNode::normalize).anyMatch(normalized::equals);
    }

    /**
     * 创建领域层节点。
     *
     * @param id 节点标识
     * @param name 节点名称
     * @param description 节点说明
     * @param fullPath 完整路径
     * @param children 子节点集合
     * @return 领域节点
     */
    static IntentNode domain(String id, String name, String description, String fullPath, List<IntentNode> children) {
        return new IntentNode(id, name, description, IntentLevel.DOMAIN, null,
                List.of(), List.of(), List.of(), fullPath, children, null, null, null);
    }

    /**
     * 创建分类层节点。
     *
     * @param id 节点标识
     * @param name 节点名称
     * @param description 节点说明
     * @param fullPath 完整路径
     * @param children 子节点集合
     * @return 分类节点
     */
    static IntentNode category(String id, String name, String description, String fullPath, List<IntentNode> children) {
        return new IntentNode(id, name, description, IntentLevel.CATEGORY, null,
                List.of(), List.of(), List.of(), fullPath, children, null, null, null);
    }

    /**
        * 创建绑定 MCP 工具的叶子主题节点。
     *
     * @param id 节点标识
     * @param name 节点名称
     * @param description 节点说明
     * @param fullPath 完整路径
        * @param mcpToolId 关联的 MCP 工具标识
     * @param keywords 关键词
     * @param aliases 别名
     * @param examples 示例
        * @return MCP 主题节点
     */
    static IntentNode mcpTopic(String id,
                               String name,
                               String description,
                               String fullPath,
                               String mcpToolId,
                               List<String> keywords,
                               List<String> aliases,
                               List<String> examples) {
        return new IntentNode(id, name, description, IntentLevel.TOPIC, IntentKind.MCP,
                keywords, examples, aliases, fullPath, List.of(), mcpToolId, null, null);
    }

    /**
        * 创建绑定知识库检索配置的叶子主题节点。
     *
     * @param id 节点标识
     * @param name 节点名称
     * @param description 节点说明
     * @param fullPath 完整路径
     * @param collectionName 知识集合名
     * @param topK 默认检索条数
     * @param keywords 关键词
     * @param aliases 别名
     * @param examples 示例
    * @return 知识库主题节点
     */
    static IntentNode kbTopic(String id,
                              String name,
                              String description,
                              String fullPath,
                              String collectionName,
                              Integer topK,
                              List<String> keywords,
                              List<String> aliases,
                              List<String> examples) {
        return new IntentNode(id, name, description, IntentLevel.TOPIC, IntentKind.KB,
                keywords, examples, aliases, fullPath, List.of(), null, collectionName, topK);
    }

    /**
        * 创建不依赖外部工具或知识库的系统类叶子主题节点。
     *
     * @param id 节点标识
     * @param name 节点名称
     * @param description 节点说明
     * @param fullPath 完整路径
     * @param keywords 关键词
     * @param aliases 别名
     * @param examples 示例
    * @return 系统主题节点
     */
    static IntentNode systemTopic(String id,
                                  String name,
                                  String description,
                                  String fullPath,
                                  List<String> keywords,
                                  List<String> aliases,
                                  List<String> examples) {
        return new IntentNode(id, name, description, IntentLevel.TOPIC, IntentKind.SYSTEM,
                keywords, examples, aliases, fullPath, List.of(), null, null, null);
    }

    /**
     * 统一节点标识的比较格式。
     *
     * @param value 原始文本
     * @return 归一化后的比较文本
     */
    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
