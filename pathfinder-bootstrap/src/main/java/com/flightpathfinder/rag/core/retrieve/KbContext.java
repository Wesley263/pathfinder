package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import java.util.List;

/**
 * 知识库分支的结构化结果。
 *
 * <p>它不仅保存命中的检索条目，也显式保存 empty 与 error 语义，避免后续阶段只能靠 items 是否为空来猜测发生了什么。</p>
 *
 * @param status 当前 KB 检索状态
 * @param summary 面向审计和 trace 的摘要
 * @param matchedIntents 本次实际触发的 KB intents
 * @param items 命中的检索条目
 * @param empty 是否为空结果
 * @param error 错误信息；无错误时为空字符串
 */
public record KbContext(
        String status,
        String summary,
        List<ResolvedIntent> matchedIntents,
        List<KbRetrievalItem> items,
        boolean empty,
        String error) {

    /**
     * 归一化 KB 上下文。
     */
    public KbContext {
        status = status == null || status.isBlank() ? "EMPTY" : status;
        summary = summary == null ? "" : summary.trim();
        matchedIntents = List.copyOf(matchedIntents == null ? List.of() : matchedIntents);
        items = List.copyOf(items == null ? List.of() : items);
        error = error == null ? "" : error.trim();
        empty = empty || items.isEmpty();
    }

    /**
     * 判断当前 KB 结果是否为错误状态。
     *
     * @return 错误状态返回 true
     */
    public boolean hasError() {
        return "ERROR".equals(status);
    }

    /**
     * 创建空 KB 结果。
     *
     * @param summary 空结果说明
     * @param matchedIntents 参与匹配的 KB intents
     * @return 结构化空结果
     */
    public static KbContext empty(String summary, List<ResolvedIntent> matchedIntents) {
        return new KbContext("EMPTY", summary, matchedIntents, List.of(), true, "");
    }

    /**
     * 创建错误 KB 结果。
     *
     * @param summary 错误摘要
     * @param matchedIntents 参与匹配的 KB intents
     * @param error 详细错误信息
     * @return 结构化错误结果
     */
    public static KbContext error(String summary, List<ResolvedIntent> matchedIntents, String error) {
        return new KbContext("ERROR", summary, matchedIntents, List.of(), true, error);
    }
}

