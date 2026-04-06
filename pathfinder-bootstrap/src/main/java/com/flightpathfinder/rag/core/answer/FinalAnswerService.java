package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;

/**
 * 最终回答阶段边界。
 *
 * 说明。
 */
public interface FinalAnswerService {

    /**
     * 说明。
     *
     * @param retrievalResult 参数说明。
     * @return 返回结果。
     */
    AnswerResult answer(RetrievalResult retrievalResult);
}

