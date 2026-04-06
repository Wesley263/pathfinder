package com.flightpathfinder.rag.core.intent;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 *
 * @param intentId 意图标识
 * @param intentName 意图名称
 * @param kind 意图所属大类
 * @param score 意图得分
 * @param question 命中该意图的子问题文本
 * @param fullPath 意图在树中的完整路径
 * @param description 意图说明
 * @param mcpToolId 参数说明。
 * @param mcpToolName 参数说明。
 * @param kbCollectionName 参数说明。
 * @param kbTopK 参数说明。
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
