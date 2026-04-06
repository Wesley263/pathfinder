package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 知识库分支检索边界。
 *
 * <p>它只关心知识检索，不感知 MCP 执行细节，这样 KB 分支后续替换成 embedding、向量库或搜索引擎时，
 * 不需要同时修改 MCP 相关逻辑。</p>
 */
public interface KnowledgeRetriever {

    /**
     * 基于当前改写结果和 KB intents 执行知识检索。
     *
     * @param rewriteResult 改写结果，提供检索文本
     * @param intentSplitResult 分流结果，提供当前 KB 意图集合
     * @return 结构化 KB 上下文，包含 empty / error 等语义
     */
    KbContext retrieve(RewriteResult rewriteResult, IntentSplitResult intentSplitResult);
}

