package com.flightpathfinder.rag.core.answer;

/**
 * 最终回答文本生成边界。
 *
 * 说明。
 * 说明。
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
