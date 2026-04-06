package com.flightpathfinder.rag.core.intent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 意图解析总结果。
 *
 * 说明。
 *
 * @param subQuestionIntents 每个子问题对应的候选意图打分列表
 * @param splitResult 参数说明。
 */
public record IntentResolution(List<SubQuestionIntent> subQuestionIntents, IntentSplitResult splitResult) {

    /**
     * 归一化意图解析结果。
     */
    public IntentResolution {
        subQuestionIntents = List.copyOf(subQuestionIntents == null ? List.of() : subQuestionIntents);
        splitResult = splitResult == null ? IntentSplitResult.empty() : splitResult;
    }

    /**
     * 创建空的意图解析结果。
     *
     * @return 不包含任何子问题和分流结果的空对象
     */
    public static IntentResolution empty() {
        return new IntentResolution(List.of(), IntentSplitResult.empty());
    }

    /**
     * 返回当前解析结果中的主意图。
     *
     * @return 全部子问题中分数最高的意图；不存在时返回空
     */
    public Optional<IntentNodeScore> primaryIntent() {
        return subQuestionIntents.stream()
                .flatMap(subQuestionIntent -> subQuestionIntent.nodeScores().stream())
                .max(Comparator.comparingDouble(IntentNodeScore::score));
    }
}
