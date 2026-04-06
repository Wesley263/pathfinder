package com.flightpathfinder.rag.core.intent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 单个意图节点的打分结果。
 *
 * <p>该类型把节点定义和最终得分绑定在一起，方便 classifier、resolver 和 audit 层沿用同一份结果。</p>
 *
 * @param node 被命中的意图节点
 * @param score 当前节点得分，范围会被约束到 0 到 1 之间
 */
public record IntentNodeScore(IntentNode node, double score) {

    /**
     * 校验节点并裁剪分数范围。
     */
    public IntentNodeScore {
        node = Objects.requireNonNull(node, "node cannot be null");
        score = clamp(score);
    }

    /**
     * 把原始分数裁剪并保留四位小数。
     *
     * @param rawScore 原始打分
     * @return 归一化后的分数
     */
    private static double clamp(double rawScore) {
        double bounded = Math.max(0.0D, Math.min(1.0D, rawScore));
        return BigDecimal.valueOf(bounded).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
