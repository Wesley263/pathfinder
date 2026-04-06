package com.flightpathfinder.rag.core.rewrite;

import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.pipeline.StageOneRagRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 第一阶段默认的轻量级问题改写实现。
 *
 * <p>它负责术语归一、子问题拆分和明显追问场景下的上下文补全。这一层故意保持确定性，
 * 让没有模型参与时主链也能稳定完成 stage one。</p>
 */
@Service
public class DefaultQuestionRewriteService implements QuestionRewriteService {

    /** 按强分隔符拆分主问题，优先保留句读和换行带来的语义边界。 */

    private static final Pattern PRIMARY_SPLIT_PATTERN = Pattern.compile("[\\n\\r?？！；;]+");
    /** 按连接词做二次拆分，用于从“另外、顺便、还有”这类串联表达里抽出子问题。 */
    private static final Pattern CONNECTOR_SPLIT_PATTERN = Pattern.compile(
            "(?:，|,)?(?:另外|顺便|顺带|还有|同时|然后|并且|再帮我|再查一下|再看一下)");
    /** 判断当前问题是否更像依赖上下文的追问，而不是完整独立问题。 */
    private static final Pattern FOLLOW_UP_PATTERN = Pattern.compile(
            "^(?:那|这个|这个呢|那个|那个呢|它|它呢|再|继续|还有|顺便|改成|换成|那如果|那签证|那机票|那价格|那风险)");

    /** 负责基础术语归一的本地规则器。 */
    private final TermNormalizer termNormalizer = new TermNormalizer();

    /**
     * 生成改写结果。
     *
     * @param request 原始问题及可选的会话记忆上下文
     * @return 同时面向展示与内部路由的改写结果
     */
    @Override
    public RewriteResult rewrite(StageOneRagRequest request) {
        String originalQuestion = request == null ? "" : request.originalQuestion();
        String rewrittenQuestion = termNormalizer.normalize(originalQuestion);
        List<String> subQuestions = splitSubQuestions(rewrittenQuestion);
        ConversationMemoryContext memoryContext = request == null ? ConversationMemoryContext.empty("") : request.memoryContext();
        if (!memoryContext.empty() && looksLikeFollowUp(rewrittenQuestion)) {
            // 只有内部路由视图拼入 memory，上层展示给用户的改写问题仍保持简洁、稳定和可审计。
            String routingQuestion = buildRoutingQuestion(rewrittenQuestion, memoryContext);
            List<String> routingSubQuestions = subQuestions.stream()
                    .map(subQuestion -> buildRoutingQuestion(subQuestion, memoryContext))
                    .toList();
            return new RewriteResult(rewrittenQuestion, subQuestions, routingQuestion, routingSubQuestions);
        }
        return new RewriteResult(rewrittenQuestion, subQuestions, rewrittenQuestion, subQuestions);
    }

    /**
     * 把改写后的问题拆成子问题列表。
     *
     * @param question 改写后的问题文本
     * @return 子问题列表；无法拆分时至少返回一个元素
     */
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
            // 先按强语义边界切，再按“另外/顺便/还有”这类连接词细分，能兼顾稳定性和拆分粒度。
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

    /**
     * 判断问题是否表现出明显的追问特征。
     *
     * @param question 当前改写问题
     * @return 如果需要借助 memory 才能正确理解，则返回 true
     */
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

    /**
     * 构造供内部路由使用的问题文本。
     *
     * @param question 当前问题或子问题
     * @param memoryContext 会话记忆上下文
     * @return 拼接了历史语义的内部路由问题文本
     */
    private String buildRoutingQuestion(String question, ConversationMemoryContext memoryContext) {
        return "Conversation context: "
                + memoryContext.routingHistoryText()
                + " || Current question: "
                + question;
    }
}
