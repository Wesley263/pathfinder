package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;

/**
 * 最终回答阶段边界。
 *
 * <p>它接收 retrieval 结果并产出用户面回答结果，但不接管 retrieval 或请求级编排。</p>
 */
public interface FinalAnswerService {

    /**
     * 基于 retrieval 输出生成最终回答。
     *
     * @param retrievalResult retrieval 阶段结果，包含 KB 与 MCP 上下文
     * @return 结构化回答结果，包含 partial 与 empty 语义
     */
    AnswerResult answer(RetrievalResult retrievalResult);
}

