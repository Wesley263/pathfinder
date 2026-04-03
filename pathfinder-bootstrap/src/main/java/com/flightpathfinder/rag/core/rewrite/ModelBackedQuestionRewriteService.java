package com.flightpathfinder.rag.core.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.infra.ai.chat.ChatRequest;
import com.flightpathfinder.infra.ai.chat.ChatResponse;
import com.flightpathfinder.infra.ai.chat.ChatService;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Model-backed rewrite service with deterministic fallback.
 */
@Service
@Primary
public class ModelBackedQuestionRewriteService implements QuestionRewriteService {

    private final DefaultQuestionRewriteService fallbackRewriteService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ModelBackedQuestionRewriteService(DefaultQuestionRewriteService fallbackRewriteService,
                                            ChatService chatService,
                                            ObjectMapper objectMapper) {
        this.fallbackRewriteService = fallbackRewriteService;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public RewriteResult rewrite(StageOneRagRequest request) {
        RewriteResult fallback = fallbackRewriteService.rewrite(request);
        StageOneRagRequest safeRequest = request == null
                ? new StageOneRagRequest("", "", "", ConversationMemoryContext.empty(""))
                : request;
        if (safeRequest.originalQuestion().isBlank()) {
            return fallback;
        }

        try {
            ChatResponse response = chatService.chat(new ChatRequest(
                    buildUserPrompt(safeRequest, fallback),
                    rewriteSystemPrompt(),
                    Map.of("stage", "rewrite", "mode", "model-backed")));
            if (response == null || response.placeholder() || response.content() == null || response.content().isBlank()) {
                return fallback;
            }

            ParsedRewrite parsed = parseRewrite(response.content());
            if (parsed == null || parsed.rewrite().isBlank()) {
                return fallback;
            }

            boolean shouldExpandForRouting = !safeRequest.memoryContext().empty()
                    && !fallback.routingQuestion().equals(fallback.rewrittenQuestion());
            String routingQuestion = shouldExpandForRouting
                    ? buildRoutingQuestion(parsed.rewrite(), safeRequest.memoryContext())
                    : parsed.rewrite();
            List<String> routingSubQuestions = shouldExpandForRouting
                    ? parsed.subQuestions().stream()
                            .map(subQuestion -> buildRoutingQuestion(subQuestion, safeRequest.memoryContext()))
                            .toList()
                    : parsed.subQuestions();
            return new RewriteResult(parsed.rewrite(), parsed.subQuestions(), routingQuestion, routingSubQuestions);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String rewriteSystemPrompt() {
        return "You rewrite user travel questions for downstream routing. "
                + "Return JSON only with fields rewrite and sub_questions. "
                + "rewrite must be a concise Chinese reformulation of the latest user question. "
                + "sub_questions must be a list of 1 to 4 Chinese questions. "
                + "Keep airport codes, dates, budgets, visa, city cost, risk and airline details precise. "
                + "Do not invent facts or parameters not implied by the question or memory.";
    }

    private String buildUserPrompt(StageOneRagRequest request, RewriteResult fallback) {
        StringBuilder builder = new StringBuilder();
        builder.append("Original question:\n")
                .append(request.originalQuestion())
                .append("\n\nFallback normalized rewrite:\n")
                .append(fallback.rewrittenQuestion())
                .append("\n\nFallback split:\n")
                .append(toJsonLikeList(fallback.subQuestions()))
                .append("\n\n");

        ConversationMemoryContext memoryContext = request.memoryContext();
        if (!memoryContext.empty()) {
            if (memoryContext.hasSummary()) {
                builder.append("Conversation summary:\n")
                        .append(memoryContext.summary().summaryText())
                        .append("\n\n");
            }
            if (!memoryContext.recentTurns().isEmpty()) {
                builder.append("Recent turns:\n");
                memoryContext.recentTurns().stream().limit(4).forEach(turn -> builder.append("- User: ")
                        .append(turn.questionForContext())
                        .append(" | Assistant: ")
                        .append(turn.answerText())
                        .append("\n"));
                builder.append("\n");
            }
        }

        builder.append("Output JSON only, for example:\n")
                .append("{\"rewrite\":\"从上海去伦敦，预算8000，顺便问签证要求\",\"sub_questions\":[\"从上海去伦敦，预算8000怎么走\",\"中国护照去英国签证要求是什么\"]}");
        return builder.toString();
    }

    private ParsedRewrite parseRewrite(String raw) throws Exception {
        String cleaned = stripMarkdownCodeFence(raw);
        JsonNode root = objectMapper.readTree(cleaned);
        String rewrite = root.path("rewrite").asText("").trim();
        List<String> subQuestions = new ArrayList<>();
        JsonNode subQuestionsNode = root.path("sub_questions");
        if (subQuestionsNode.isArray()) {
            for (JsonNode item : subQuestionsNode) {
                String value = item.asText("").trim();
                if (!value.isBlank()) {
                    subQuestions.add(value);
                }
            }
        }
        if (subQuestions.isEmpty() && !rewrite.isBlank()) {
            subQuestions = List.of(rewrite);
        }
        return new ParsedRewrite(rewrite, List.copyOf(subQuestions));
    }

    private String stripMarkdownCodeFence(String raw) {
        String cleaned = raw == null ? "" : raw.trim();
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline >= 0) {
                cleaned = cleaned.substring(firstNewline + 1, cleaned.length() - 3).trim();
            }
        }
        return cleaned;
    }

    private String buildRoutingQuestion(String question, ConversationMemoryContext memoryContext) {
        return "Conversation context: " + memoryContext.routingHistoryText() + " || Current question: " + question;
    }

    private String toJsonLikeList(List<String> values) {
        List<String> safeValues = values == null ? List.of() : values;
        List<String> quoted = safeValues.stream().map(value -> '"' + value + '"').toList();
        return '[' + String.join(", ", quoted) + ']';
    }

    private record ParsedRewrite(String rewrite, List<String> subQuestions) {
    }
}