package com.flightpathfinder.rag.core.retrieve;

/**
 * 单条 KB 检索命中结果。
 *
 * <p>它保留意图、集合、主题、来源和排序信息，便于 final answer、audit 和 trace 从管理视角查看“这条知识为什么会进入回答”。</p>
 *
 * @param intentId 触发该条目的意图标识
 * @param intentName 触发该条目的意图名称
 * @param question 对应的子问题文本
 * @param collectionName 知识集合名
 * @param topic 主题名
 * @param source 条目来源标识
 * @param documentId 文档标识
 * @param title 文档标题
 * @param content 文档内容摘要
 * @param score 检索得分
 * @param rank 当前意图下的排序名次
 */
public record KbRetrievalItem(
        String intentId,
        String intentName,
        String question,
        String collectionName,
        String topic,
        String source,
        String documentId,
        String title,
        String content,
        double score,
        int rank) {

    /**
     * 归一化 KB 检索条目中的文本字段。
     */
    public KbRetrievalItem {
        intentId = intentId == null ? "" : intentId.trim();
        intentName = intentName == null ? "" : intentName.trim();
        question = question == null ? "" : question.trim();
        collectionName = collectionName == null ? "" : collectionName.trim();
        topic = topic == null ? "" : topic.trim();
        source = source == null ? "" : source.trim();
        documentId = documentId == null ? "" : documentId.trim();
        title = title == null ? "" : title.trim();
        content = content == null ? "" : content.trim();
    }
}
