package com.flightpathfinder.rag.core.rewrite;

import java.util.ArrayList;
import java.util.List;

/**
 * 问题改写阶段的标准结果。
 *
 * 同时包含展示视角与内部路由视角的改写结果。
 *
 * @param rewrittenQuestion 展示给后续审计与响应层的改写问题
 * @param subQuestions 展示视角下的子问题拆分结果
 * @param routingQuestion 供意图识别和路由使用的内部问题文本
 * @param routingSubQuestions 供意图识别和路由使用的内部子问题拆分结果
 */
public record RewriteResult(
        String rewrittenQuestion,
        List<String> subQuestions,
        String routingQuestion,
        List<String> routingSubQuestions) {

    /**
     * 归一化改写结果并补齐兜底子问题。
     */
    public RewriteResult {
        rewrittenQuestion = rewrittenQuestion == null ? "" : rewrittenQuestion.trim();
        routingQuestion = routingQuestion == null || routingQuestion.isBlank()
                ? rewrittenQuestion
                : routingQuestion.trim();

        List<String> sanitizedSubQuestions = new ArrayList<>();
        for (String subQuestion : subQuestions == null ? List.<String>of() : subQuestions) {
            if (subQuestion == null) {
                continue;
            }
            String candidate = subQuestion.trim();
            if (!candidate.isBlank()) {
                sanitizedSubQuestions.add(candidate);
            }
        }

        if (sanitizedSubQuestions.isEmpty()) {
            sanitizedSubQuestions = rewrittenQuestion.isBlank()
                    ? new ArrayList<>(List.of(""))
                    : new ArrayList<>(List.of(rewrittenQuestion));
        }
        subQuestions = List.copyOf(sanitizedSubQuestions);

        List<String> sanitizedRoutingSubQuestions = new ArrayList<>();
        for (String subQuestion : routingSubQuestions == null ? List.<String>of() : routingSubQuestions) {
            if (subQuestion == null) {
                continue;
            }
            String candidate = subQuestion.trim();
            if (!candidate.isBlank()) {
                sanitizedRoutingSubQuestions.add(candidate);
            }
        }
        if (sanitizedRoutingSubQuestions.isEmpty()) {
            sanitizedRoutingSubQuestions = routingQuestion.isBlank()
                    ? new ArrayList<>(subQuestions)
                    : new ArrayList<>(List.of(routingQuestion));
        }
        routingSubQuestions = List.copyOf(sanitizedRoutingSubQuestions);
    }
}

