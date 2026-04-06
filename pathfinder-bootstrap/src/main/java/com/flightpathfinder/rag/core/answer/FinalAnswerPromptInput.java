package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.retrieve.KbContext;
import com.flightpathfinder.rag.core.retrieve.McpContext;
import java.util.List;

/**
 * 最终回答阶段的标准输入模型。
 *
 * 说明。
 *
 * @param rewrittenQuestion 当前问题的展示型改写文本
 * @param intentSplitResult 当前问题的分流结果
 * @param kbContext 参数说明。
 * @param mcpContext 参数说明。
 * @param memoryContext 当前会话记忆上下文
 * @param evidenceSummaries 已汇总的证据摘要
 * @param partial 是否应按部分回答处理
 * @param snapshotMissAffected 是否受到图快照缺失影响
 * @param empty 是否没有可用于回答的上下文
 */
public record FinalAnswerPromptInput(
        String rewrittenQuestion,
        IntentSplitResult intentSplitResult,
        KbContext kbContext,
        McpContext mcpContext,
        ConversationMemoryContext memoryContext,
        List<AnswerEvidenceSummary> evidenceSummaries,
        boolean partial,
        boolean snapshotMissAffected,
        boolean empty) {

    /**
     * 说明。
     */
    public FinalAnswerPromptInput {
        rewrittenQuestion = rewrittenQuestion == null ? "" : rewrittenQuestion.trim();
        intentSplitResult = intentSplitResult == null ? IntentSplitResult.empty() : intentSplitResult;
        kbContext = kbContext == null
                ? KbContext.empty("no KB context", List.of())
                : kbContext;
        mcpContext = mcpContext == null
                ? McpContext.skipped("no MCP context")
                : mcpContext;
        memoryContext = memoryContext == null
                ? ConversationMemoryContext.empty("")
                : memoryContext;
        evidenceSummaries = List.copyOf(evidenceSummaries == null ? List.of() : evidenceSummaries);
    }
}

