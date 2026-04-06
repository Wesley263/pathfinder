package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 知识库分支检索边界。
 *
 * 根据改写结果和 KB 意图分流结果返回结构化检索上下文。
 */
public interface KnowledgeRetriever {

    /**
     * 执行知识库分支检索。
     *
     * @param rewriteResult 改写结果，提供检索文本
     * @param intentSplitResult 意图分流结果
     * @return 知识库检索上下文
     */
    KbContext retrieve(RewriteResult rewriteResult, IntentSplitResult intentSplitResult);
}

