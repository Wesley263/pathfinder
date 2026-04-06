package com.flightpathfinder.rag.core.rewrite;

import java.util.ArrayList;
import java.util.List;

/**
 * 问题改写阶段的标准结果。
 *
 * <p>这里同时保留两套视图：一套面向用户与审计展示，一套面向内部路由。这样既能保证返回给调用方的
 * 保证重写问题（rewritten question）稳定可读，也能在需要时把 memory 上下文拼接进内部 routing 文本。</p>
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
     *
     * <p>这里保证四个字段都具备稳定语义，避免下游在“无子问题”“无内部路由文本”这类场景下再做重复兜底。</p>
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

