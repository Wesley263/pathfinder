package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface McpContextExecutor {

    /**
     * 说明。
     *
     * @param rewriteResult 改写结果，主要用于参数抽取
     * @param intentSplitResult 参数说明。
     * @return 返回结果。
     */
    McpContext execute(RewriteResult rewriteResult, IntentSplitResult intentSplitResult);
}

