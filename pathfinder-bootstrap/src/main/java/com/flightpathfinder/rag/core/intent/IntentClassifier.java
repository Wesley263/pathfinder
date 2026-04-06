package com.flightpathfinder.rag.core.intent;

import java.util.List;

/**
 * 单个问题文本的意图分类边界。
 *
 * <p>它只负责给候选意图打分，不负责多个子问题之间的汇总和最终分流。
 * 这样分类器可以专注“识别”，而 resolver 专注“决策”。</p>
 */
public interface IntentClassifier {

    /**
     * 为单个问题文本计算候选意图分数。
     *
     * @param question 改写后的问题或子问题文本
     * @return 按分数排序的候选叶子意图列表
     */
    List<IntentNodeScore> classifyTargets(String question);
}
