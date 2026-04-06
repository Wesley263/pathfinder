package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;

/**
 * 最终回答输入装配边界。
 *
 * 说明。
 * 说明。
 */
public interface FinalAnswerAssembler {

    /**
     * 说明。
     *
     * @param retrievalResult 参数说明。
     * @return 统一的回答输入模型
     */
    FinalAnswerPromptInput assemble(RetrievalResult retrievalResult);
}

