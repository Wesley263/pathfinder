package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 检索阶段中的 MCP 执行边界。
 *
 * <p>它负责把 MCP intents 转成真实的工具调用，并返回结构化 McpContext，
 * 但不会越界去接管 final answer 生成。</p>
 */
public interface McpContextExecutor {

    /**
     * 执行当前问题命中的 MCP intents。
     *
     * @param rewriteResult 改写结果，主要用于参数抽取
     * @param intentSplitResult 分流结果，主要提供 MCP intents
     * @return 结构化 MCP 上下文，包含 partial、miss 和 error 语义
     */
    McpContext execute(RewriteResult rewriteResult, IntentSplitResult intentSplitResult);
}

