package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import java.util.List;

/**
 * 知识库分支的结构化结果。
 *
 * 说明。
 *
 * @param status 参数说明。
 * @param summary 参数说明。
 * @param matchedIntents 参数说明。
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
     * 说明。
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
     * 说明。
     *
     * @return 返回结果。
     */
    public boolean hasError() {
        return "ERROR".equals(status);
    }

    /**
     * 说明。
     *
     * @param summary 空结果说明
     * @param matchedIntents 参数说明。
     * @return 结构化空结果
     */
    public static KbContext empty(String summary, List<ResolvedIntent> matchedIntents) {
        return new KbContext("EMPTY", summary, matchedIntents, List.of(), true, "");
    }

    /**
     * 说明。
     *
     * @param summary 错误摘要
     * @param matchedIntents 参数说明。
     * @param error 详细错误信息
     * @return 结构化错误结果
     */
    public static KbContext error(String summary, List<ResolvedIntent> matchedIntents, String error) {
        return new KbContext("ERROR", summary, matchedIntents, List.of(), true, error);
    }
}

