package com.flightpathfinder.rag.core.rewrite;

import java.util.ArrayList;
import java.util.List;

public record RewriteResult(
        String rewrittenQuestion,
        List<String> subQuestions,
        String routingQuestion,
        List<String> routingSubQuestions) {

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
