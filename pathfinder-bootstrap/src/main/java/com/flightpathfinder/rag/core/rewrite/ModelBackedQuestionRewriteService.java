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
 * 基于模型的改写服务实现。
 *
 * <p>它在确定性 rewrite 之上增加模型能力，但仍保留本地 fallback。这样即使模型不可用、
 * 返回空结果或格式不合法，第一阶段也不会失去稳定可运行的改写能力。</p>
 */
@Service
@Primary
public class ModelBackedQuestionRewriteService implements QuestionRewriteService {

    /** 确定性改写服务，作为模型不可用时的兜底实现。 */
    private final DefaultQuestionRewriteService fallbackRewriteService;
    /** 面向 2.0 AI 基础设施的聊天模型调用入口。 */
    private final ChatService chatService;
    /** 用于解析模型返回 JSON 的对象映射器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造模型增强版改写服务。
     *
     * @param fallbackRewriteService 确定性兜底改写服务
     * @param chatService 模型对话服务
     * @param objectMapper JSON 解析器
     */
    public ModelBackedQuestionRewriteService(DefaultQuestionRewriteService fallbackRewriteService,
                                            ChatService chatService,
                                            ObjectMapper objectMapper) {
        this.fallbackRewriteService = fallbackRewriteService;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    /**
     * 先执行本地稳定改写，再尝试用模型生成更自然的改写结果。
     *
     * @param request 原始问题及请求上下文
     * @return 优先使用模型结果，模型不可用时回落到确定性改写结果
     */
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

    /**
     * 构造模型系统提示词。
     *
     * @return 约束模型输出结构和改写边界的系统提示词
     */
    private String rewriteSystemPrompt() {
        return "You rewrite user travel questions for downstream routing. "
                + "Return JSON only with fields rewrite and sub_questions. "
                + "rewrite must be a concise Chinese reformulation of the latest user question. "
                + "sub_questions must be a list of 1 to 4 Chinese questions. "
                + "Keep airport codes, dates, budgets, visa, city cost, risk and airline details precise. "
                + "Do not invent facts or parameters not implied by the question or memory.";
    }

    /**
     * 构造模型用户提示词。
     *
     * @param request 原始请求
     * @param fallback 确定性改写结果，用作模型参考与兜底基线
     * @return 提交给模型的用户提示词
     */
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

    /**
     * 解析模型返回的 JSON 改写结果。
     *
     * @param raw 模型原始文本输出
     * @return 解析后的改写结果；缺少子问题时会回填为单问题列表
     * @throws Exception 当 JSON 不合法或无法解析时抛出异常
     */
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

    /**
     * 去掉模型返回内容可能携带的 Markdown 代码块包裹。
     *
     * @param raw 模型原始输出
     * @return 适合 JSON 解析的纯文本内容
     */
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

    /**
     * 构造内部路由问题文本。
     *
     * @param question 当前问题或子问题
     * @param memoryContext 会话记忆上下文
     * @return 拼入上下文后的路由文本
     */
    private String buildRoutingQuestion(String question, ConversationMemoryContext memoryContext) {
        return "Conversation context: " + memoryContext.routingHistoryText() + " || Current question: " + question;
    }

    /**
     * 把字符串列表格式化成接近 JSON 数组的展示文本。
     *
     * @param values 待格式化的字符串列表
     * @return 便于模型理解的数组文本
     */
    private String toJsonLikeList(List<String> values) {
        List<String> safeValues = values == null ? List.of() : values;
        List<String> quoted = safeValues.stream().map(value -> '"' + value + '"').toList();
        return '[' + String.join(", ", quoted) + ']';
    }

    /**
     * 模型改写解析结果。
     *
     * @param rewrite 模型给出的主改写问题
     * @param subQuestions 模型给出的子问题列表
     */
    private record ParsedRewrite(String rewrite, List<String> subQuestions) {
    }
}
