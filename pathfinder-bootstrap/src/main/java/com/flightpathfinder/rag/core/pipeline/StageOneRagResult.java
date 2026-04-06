package com.flightpathfinder.rag.core.pipeline;

import com.flightpathfinder.rag.core.intent.IntentResolution;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.memory.ConversationMemoryContext;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 第一阶段主链的标准输出模型。
 *
 * <p>它同时保留改写结果、意图解析明细和最终分流结果，目的是让 retrieval 既能消费最终 hand-off，
 * 又能在需要时回看第一阶段内部是如何得到这个结论的。</p>
 *
 * @param rewriteResult 改写结果，包含展示用问题和内部路由用问题
 * @param intentResolution 意图解析全量结果，包含子问题级别的打分视图
 * @param intentSplitResult 面向 retrieval 的 KB/MCP/SYSTEM 汇总分流结果
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
     * <p>这里为各子结果补默认空对象，确保后续 retrieval、answer 和 audit 层不需要反复做空值防御。</p>
     */
    public StageOneRagResult {
        rewriteResult = rewriteResult == null ? new RewriteResult("", null, null, null) : rewriteResult;
        intentResolution = intentResolution == null ? IntentResolution.empty() : intentResolution;
        intentSplitResult = intentSplitResult == null ? intentResolution.splitResult() : intentSplitResult;
        memoryContext = memoryContext == null ? ConversationMemoryContext.empty("") : memoryContext;
    }
}
