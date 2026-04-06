package com.flightpathfinder.rag.core.intent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 单个子问题的意图识别结果。
 *
 * 说明。
 *
 * @param question 当前子问题文本
 * @param nodeScores 当前子问题对应的候选意图打分列表
 */
public record SubQuestionIntent(String question, List<IntentNodeScore> nodeScores) {

    /**
     * 归一化子问题意图结果。
     */
    public SubQuestionIntent {
        question = question == null ? "" : question.trim();
        nodeScores = List.copyOf(nodeScores == null ? List.of() : nodeScores);
    }

    /**
     * 返回当前子问题分数最高的主意图。
     *
     * @return 主意图；不存在时返回空
     */
    public Optional<IntentNodeScore> primaryIntent() {
        return nodeScores.stream().max(Comparator.comparingDouble(IntentNodeScore::score));
    }
}
