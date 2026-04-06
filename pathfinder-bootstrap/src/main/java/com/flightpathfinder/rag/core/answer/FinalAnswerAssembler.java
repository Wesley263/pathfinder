package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;

/**
 * 最终回答输入装配边界。
 *
 * <p>它负责把 retrieval 输出整理成统一的回答输入模型，让证据收集和文本生成保持分层，
 * 后续无论换 prompt 还是换模型，都不必回改 retrieval 逻辑。</p>
 */
public interface FinalAnswerAssembler {

    /**
     * 把 retrieval 结果转换成回答生成可直接消费的输入模型。
     *
     * @param retrievalResult retrieval 阶段输出
     * @return 统一的回答输入模型
     */
    FinalAnswerPromptInput assemble(RetrievalResult retrievalResult);
}

