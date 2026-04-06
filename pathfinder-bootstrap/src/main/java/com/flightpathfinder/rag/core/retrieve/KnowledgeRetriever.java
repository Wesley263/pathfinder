package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 知识库分支检索边界。
 *
 * 说明。
 * 说明。
 */
public interface KnowledgeRetriever {

    /**
     * 说明。
     *
     * @param rewriteResult 改写结果，提供检索文本
     * @param intentSplitResult 参数说明。
     * @return 返回结果。
     */
    KbContext retrieve(RewriteResult rewriteResult, IntentSplitResult intentSplitResult);
}

