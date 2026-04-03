package com.flightpathfinder.rag.core.rewrite;

import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Lightweight rewrite implementation for stage one.
 *
 * <p>This service only handles normalization, sub-question splitting, and obvious follow-up
 * expansion. Heavier reasoning still belongs to later retrieval and answer stages.</p>
 */
@Service
public class DefaultQuestionRewriteService implements QuestionRewriteService {

    private static final Pattern PRIMARY_SPLIT_PATTERN = Pattern.compile("[\\n\\r?？！；;]+");
    private static final Pattern CONNECTOR_SPLIT_PATTERN = Pattern.compile(
            "(?:，|,)?(?:另外|顺便|顺带|还有|同时|然后|并且|再帮我|再查一下|再看一下)");
    private static final Pattern FOLLOW_UP_PATTERN = Pattern.compile(
            "^(?:那|这个|这个呢|那个|那个呢|它|它呢|再|继续|还有|顺便|改成|换成|那如果|那签证|那机票|那价格|那风险)");

    private final TermNormalizer termNormalizer = new TermNormalizer();

    /**
     * Produces both a user-facing rewritten question and an internal routing view.
     *
     * @param request original question plus optional memory context
     * @return rewrite result for display and downstream routing
     */
    @Override
    public RewriteResult rewrite(StageOneRagRequest request) {
        String originalQuestion = request == null ? "" : request.originalQuestion();
        String rewrittenQuestion = termNormalizer.normalize(originalQuestion);
        List<String> subQuestions = splitSubQuestions(rewrittenQuestion);
        ConversationMemoryContext memoryContext = request == null ? ConversationMemoryContext.empty("") : request.memoryContext();
        if (!memoryContext.empty() && looksLikeFollowUp(rewrittenQuestion)) {
            // Only the internal routing view gets memory expansion so the displayed rewritten
            // question stays stable while follow-up disambiguation still works downstream.
            String routingQuestion = buildRoutingQuestion(rewrittenQuestion, memoryContext);
            List<String> routingSubQuestions = subQuestions.stream()
                    .map(subQuestion -> buildRoutingQuestion(subQuestion, memoryContext))
                    .toList();
            return new RewriteResult(rewrittenQuestion, subQuestions, routingQuestion, routingSubQuestions);
        }
        return new RewriteResult(rewrittenQuestion, subQuestions, rewrittenQuestion, subQuestions);
    }

    private List<String> splitSubQuestions(String question) {
        String rawQuestion = question == null ? "" : question.trim();
        if (rawQuestion.isBlank()) {
            return List.of("");
        }

        List<String> segments = new ArrayList<>();
        for (String primarySegment : PRIMARY_SPLIT_PATTERN.split(rawQuestion)) {
            String trimmedPrimarySegment = primarySegment.trim();
            if (trimmedPrimarySegment.isBlank()) {
                continue;
            }
            // Two-phase splitting keeps strong punctuation boundaries first, then peels off
            // conversational connectors such as "also" without discarding the segment itself.
            String[] secondarySegments = CONNECTOR_SPLIT_PATTERN.split(trimmedPrimarySegment);
            for (String secondarySegment : secondarySegments) {
                String trimmedSecondarySegment = secondarySegment.trim();
                if (!trimmedSecondarySegment.isBlank()) {
                    segments.add(trimmedSecondarySegment);
                }
            }
        }
        return segments.isEmpty() ? List.of(rawQuestion) : List.copyOf(segments);
    }

    private boolean looksLikeFollowUp(String question) {
        String safeQuestion = question == null ? "" : question.trim();
        if (safeQuestion.isBlank()) {
            return false;
        }
        return FOLLOW_UP_PATTERN.matcher(safeQuestion).find()
                || (safeQuestion.length() <= 6 && safeQuestion.endsWith("呢"))
                || safeQuestion.contains("上一个")
                || safeQuestion.contains("刚才")
                || safeQuestion.contains("之前")
                || safeQuestion.contains("同一个")
                || safeQuestion.contains("同样");
    }

    private String buildRoutingQuestion(String question, ConversationMemoryContext memoryContext) {
        return "Conversation context: "
                + memoryContext.routingHistoryText()
                + " || Current question: "
                + question;
    }
}
