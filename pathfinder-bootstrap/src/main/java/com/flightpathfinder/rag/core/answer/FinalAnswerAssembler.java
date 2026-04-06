package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;

/**
 * 最终回答输入装配边界。
 *
 * 将检索阶段结果装配为回答生成阶段可消费的统一输入。
 */
public interface FinalAnswerAssembler {

    /**
     * 装配最终回答输入。
     *
     * @param retrievalResult 检索阶段结果
     * @return 统一的回答输入模型
     */
    FinalAnswerPromptInput assemble(RetrievalResult retrievalResult);
}

