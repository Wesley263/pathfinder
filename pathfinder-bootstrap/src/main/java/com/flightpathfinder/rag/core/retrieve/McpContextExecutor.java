package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * MCP 上下文执行边界。
 *
 * 将第一阶段意图分流结果转换为可消费的 MCP 执行上下文。
 */
public interface McpContextExecutor {

    /**
     * 执行 MCP 调用并聚合执行上下文。
     *
     * @param rewriteResult 改写结果，主要用于参数抽取
     * @param intentSplitResult 意图分流结果
     * @return MCP 执行上下文
     */
    McpContext execute(RewriteResult rewriteResult, IntentSplitResult intentSplitResult);
}

