package com.flightpathfinder.rag.core.intent;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 意图树节点定义。
 *
 * <p>同一个类型同时描述树结构与叶子节点元数据，目的是让分类器、resolver 和 overview 场景都围绕一套
 * 稳定的意图契约工作，而不是为“树结构”和“分类目标”维护两套模型。</p>
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
 * @param mcpToolId MCP 主题节点绑定的 toolId
 * @param collectionName KB 主题节点绑定的知识集合名
 * @param topK KB 主题节点默认检索条数
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
     * <p>这里确保主题节点一定声明 kind，同时把集合字段都转换为不可变集合，避免意图树在运行期被外部误改。</p>
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
     * @return 当没有子节点时返回 true
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * 判断给定标识是否能匹配当前节点。
     *
     * @param candidateId 待匹配的节点标识、别名或 toolId
     * @return 匹配成功返回 true
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
     * 创建 MCP 主题节点。
     *
     * @param id 节点标识
     * @param name 节点名称
     * @param description 节点说明
     * @param fullPath 完整路径
     * @param mcpToolId 绑定的 MCP toolId
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
     * 创建 KB 主题节点。
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
     * @return KB 主题节点
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
     * 创建 SYSTEM 主题节点。
     *
     * @param id 节点标识
     * @param name 节点名称
     * @param description 节点说明
     * @param fullPath 完整路径
     * @param keywords 关键词
     * @param aliases 别名
     * @param examples 示例
     * @return SYSTEM 主题节点
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
