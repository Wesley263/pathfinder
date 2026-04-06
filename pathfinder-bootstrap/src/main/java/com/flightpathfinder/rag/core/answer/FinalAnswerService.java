package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;

/**
 * 最终回答阶段边界。
 *
 * 定义从检索结果生成最终回答结果的统一入口。
 */
public interface FinalAnswerService {

    /**
     * 生成最终回答。
     *
     * @param retrievalResult 检索阶段结果
     * @return 最终回答结果
     */
    AnswerResult answer(RetrievalResult retrievalResult);
}

