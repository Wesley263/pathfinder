package com.flightpathfinder.rag.core.intent;

/**
 * 面向 retrieval 阶段的已解析意图。
 *
 * <p>这个模型把分类阶段的节点信息收口成后续真正需要消费的字段，例如 mcpToolId、KB collection 和 topK，
 * 避免 retrieval 继续直接依赖完整意图树结构。</p>
 *
 * @param intentId 意图标识
 * @param intentName 意图名称
 * @param kind 意图所属大类
 * @param score 意图得分
 * @param question 命中该意图的子问题文本
 * @param fullPath 意图在树中的完整路径
 * @param description 意图说明
 * @param mcpToolId 绑定的 MCP toolId
 * @param mcpToolName MCP 工具显示名称
 * @param kbCollectionName 绑定的 KB 集合名
 * @param kbTopK 绑定的 KB 默认检索条数
 */
public record ResolvedIntent(
        String intentId,
        String intentName,
        IntentKind kind,
        double score,
        String question,
        String fullPath,
        String description,
        String mcpToolId,
        String mcpToolName,
        String kbCollectionName,
        Integer kbTopK) {
}
