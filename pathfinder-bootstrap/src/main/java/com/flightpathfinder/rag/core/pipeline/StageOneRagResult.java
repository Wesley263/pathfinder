package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 第一阶段主链的标准输出模型。
 *
 * 说明。
 * 说明。
 *
 * @param rewriteResult 改写结果，包含展示用问题和内部路由用问题
 * @param intentResolution 意图解析全量结果，包含子问题级别的打分视图
 * @param intentSplitResult 参数说明。
 * @param memoryContext 本次请求实际参与第一阶段的会话记忆上下文
 */
public record StageOneRagResult(
        RewriteResult rewriteResult,
        IntentResolution intentResolution,
        IntentSplitResult intentSplitResult,
        ConversationMemoryContext memoryContext) {

    /**
     * 归一化第一阶段结果。
     *
     * 说明。
     */
    public StageOneRagResult {
        rewriteResult = rewriteResult == null ? new RewriteResult("", null, null, null) : rewriteResult;
        intentResolution = intentResolution == null ? IntentResolution.empty() : intentResolution;
        intentSplitResult = intentSplitResult == null ? intentResolution.splitResult() : intentSplitResult;
        memoryContext = memoryContext == null ? ConversationMemoryContext.empty("") : memoryContext;
    }
}
