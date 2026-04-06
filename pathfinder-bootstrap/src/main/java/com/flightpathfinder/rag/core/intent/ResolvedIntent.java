package com.flightpathfinder.rag.core.intent;

/**
 * 已解析意图结果。
 *
 * 表示某个子问题命中的意图及其执行所需元信息。
 *
 * @param intentId 意图标识
 * @param intentName 意图名称
 * @param kind 意图所属大类
 * @param score 意图得分
 * @param question 命中该意图的子问题文本
 * @param fullPath 意图在树中的完整路径
 * @param description 意图说明
 * @param mcpToolId MCP 工具标识
 * @param mcpToolName MCP 工具名称
 * @param kbCollectionName 知识库集合名
 * @param kbTopK 知识库默认检索条数
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

