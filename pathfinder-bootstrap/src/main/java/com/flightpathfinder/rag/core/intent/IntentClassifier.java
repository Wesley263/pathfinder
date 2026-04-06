package com.flightpathfinder.rag.core.intent;

import java.util.List;

/**
 * 单个问题文本的意图分类边界。
 *
 * 说明。
 * 说明。
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
