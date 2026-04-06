package com.flightpathfinder.rag.core.answer;

/**
 * 最终回答文本生成边界。
 *
 * <p>它只负责把已经装配好的输入模型转成用户可读文本，让证据收集和文本组织保持分层，
 * 后续可平滑替换为 LLM 或更复杂的 prompt 方案。</p>
 */
public interface FinalAnswerTextComposer {

    /**
     * 根据标准化回答输入生成最终文本。
     *
     * @param promptInput 标准化后的回答输入
     * @return 面向调用方输出的最终回答文本
     */
    String compose(FinalAnswerPromptInput promptInput);
}
