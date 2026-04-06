package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.infra.ai.chat.ChatRequest;
import com.flightpathfinder.infra.ai.chat.ChatResponse;
import com.flightpathfinder.infra.ai.chat.ChatService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
@Service
@Primary
public class ModelBackedFinalAnswerTextComposer implements FinalAnswerTextComposer {

    /** 确定性文本生成器，作为兜底实现。 */
    private final DefaultFinalAnswerTextComposer deterministicComposer;
    /** 模型调用入口。 */
    private final ChatService chatService;

    /**
     * 构造模型增强版文本生成器。
     *
     * @param deterministicComposer 确定性兜底生成器
     * @param chatService 模型调用服务
     */
    public ModelBackedFinalAnswerTextComposer(DefaultFinalAnswerTextComposer deterministicComposer,
                                              ChatService chatService) {
        this.deterministicComposer = deterministicComposer;
        this.chatService = chatService;
    }

    /**
     * 生成最终回答文本。
     *
     * 说明。
     *
     * @param promptInput 回答输入
     * @return 最终回答文本
     */
    @Override
    public String compose(FinalAnswerPromptInput promptInput) {
        String fallbackText = deterministicComposer.compose(promptInput);
        if (promptInput == null || promptInput.empty()) {
            return fallbackText;
        }

        String systemPrompt = "You are the final answer composer for Flight Pathfinder. "
                + "Use only the provided MCP and KB evidence. "
                + "Answer in concise Chinese. "
                + "If information is partial or missing, say so explicitly instead of inventing facts.";
        String userPrompt = buildUserPrompt(promptInput, fallbackText);
        ChatResponse response = chatService.chat(new ChatRequest(
                userPrompt,
                systemPrompt,
                Map.of("stage", "final-answer", "mode", "model-backed")));
        if (response == null || response.placeholder() || response.content() == null || response.content().isBlank()) {
            return fallbackText;
        }
        return response.content().trim();
    }

    /**
     * 构造提交给模型的用户提示词。
     *
     * @param promptInput 回答输入
     * @param fallbackText 确定性草稿
     * @return 用户提示词
     */
    private String buildUserPrompt(FinalAnswerPromptInput promptInput, String fallbackText) {
        StringBuilder builder = new StringBuilder();
        builder.append("Question:\n").append(promptInput.rewrittenQuestion()).append("\n\n");

        if (!promptInput.memoryContext().summary().summaryText().isBlank()) {
            builder.append("Conversation summary:\n")
                    .append(promptInput.memoryContext().summary().summaryText())
                    .append("\n\n");
        }

        if (!promptInput.memoryContext().recentTurns().isEmpty()) {
            builder.append("Recent turns:\n");
            promptInput.memoryContext().recentTurns().stream()
                    .limit(4)
                    .forEach(turn -> builder.append("- Q: ")
                            .append(turn.questionForContext())
                            .append(" | A: ")
                            .append(turn.answerText())
                            .append("\n"));
            builder.append("\n");
        }

        builder.append("Evidence summaries:\n");
        List<AnswerEvidenceSummary> evidence = promptInput.evidenceSummaries();
        if (evidence.isEmpty()) {
            builder.append("- none\n");
        } else {
            evidence.forEach(item -> builder.append("- [")
                    .append(item.type())
                    .append("] ")
                    .append(item.label())
                    .append(" | status=")
                    .append(item.status())
                    .append(" | snippet=")
                    .append(item.snippet())
                    .append("\n"));
        }

        builder.append("\nCurrent retrieval state:\n")
                .append("- partial=").append(promptInput.partial()).append("\n")
                .append("- snapshotMissAffected=").append(promptInput.snapshotMissAffected()).append("\n\n")
                .append("Deterministic draft:\n")
                .append(fallbackText)
                .append("\n\nPlease write the final user-facing answer now.");
        return builder.toString();
    }
}
