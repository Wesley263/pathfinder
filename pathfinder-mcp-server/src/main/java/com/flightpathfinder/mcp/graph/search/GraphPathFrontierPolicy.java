package com.flightpathfinder.mcp.graph.search;

/**
 * frontier 控制策略。
 *
 * 统一定义搜索扩展、候选池与绕路裁剪相关阈值。
 */
final class GraphPathFrontierPolicy {

    /** frontier 最大容量。 */
    private final int maxFrontierSize;
    /** 最大扩展次数。 */
    private final int maxExpansions;
    /** 候选池容量。 */
    private final int candidatePoolSize;
    /** 触发提前停止所需的最小候选数。 */
    private final int earlyStopCandidateCount;
    /** Pareto 选择阶段的最小保留数量。 */
    private final int minimumParetoSelectionCount;
    /** 候选准入松弛量。 */
    private final double candidateAdmissionSlack;
    /** frontier 收敛判定松弛量。 */
    private final double frontierClosureSlack;
    /** 绕路基础阈值。 */
    private final double detourBaseThreshold;
    /** 额外航段对应的绕路阈值增量。 */
    private final double detourPerExtraSegmentThreshold;

    /**
        * 构造 frontier 控制策略。
     */
    private GraphPathFrontierPolicy(int maxFrontierSize,
                                    int maxExpansions,
                                    int candidatePoolSize,
                                    int earlyStopCandidateCount,
                                    int minimumParetoSelectionCount,
                                    double candidateAdmissionSlack,
                                    double frontierClosureSlack,
                                    double detourBaseThreshold,
                                    double detourPerExtraSegmentThreshold) {
        this.maxFrontierSize = maxFrontierSize;
        this.maxExpansions = maxExpansions;
        this.candidatePoolSize = candidatePoolSize;
        this.earlyStopCandidateCount = earlyStopCandidateCount;
        this.minimumParetoSelectionCount = minimumParetoSelectionCount;
        this.candidateAdmissionSlack = candidateAdmissionSlack;
        this.frontierClosureSlack = frontierClosureSlack;
        this.detourBaseThreshold = detourBaseThreshold;
        this.detourPerExtraSegmentThreshold = detourPerExtraSegmentThreshold;
    }

    /**
        * 根据请求生成默认 frontier 策略。
     */
    static GraphPathFrontierPolicy defaultPolicy(GraphPathSearchRequest request) {
        int maxFrontierSize = Math.max(320, request.topK() * request.maxSegments() * 160);
        int candidatePoolSize = Math.max(80, request.topK() * 24);
        int maxExpansions = Math.max(1600, maxFrontierSize * 8);
        int earlyStopCandidateCount = Math.max(request.topK() * 6, Math.min((candidatePoolSize * 3) / 4, 60));
        int minimumParetoSelectionCount = Math.max(request.topK() * 5, request.topK());
        // 参数按 topK 与最大航段动态缩放，兼顾召回与计算成本。
        return new GraphPathFrontierPolicy(
                maxFrontierSize,
                maxExpansions,
                candidatePoolSize,
                earlyStopCandidateCount,
                minimumParetoSelectionCount,
                0.05,
                0.03,
                1.95,
                0.35);
    }

    int maxFrontierSize() {
        return maxFrontierSize;
    }

    int maxExpansions() {
        return maxExpansions;
    }

    int candidatePoolSize() {
        return candidatePoolSize;
    }

    int earlyStopCandidateCount() {
        return earlyStopCandidateCount;
    }

    int minimumParetoSelectionCount() {
        return minimumParetoSelectionCount;
    }

    double candidateAdmissionSlack() {
        return candidateAdmissionSlack;
    }

    double frontierClosureSlack() {
        return frontierClosureSlack;
    }

    double detourBaseThreshold() {
        return detourBaseThreshold;
    }

    double detourPerExtraSegmentThreshold() {
        return detourPerExtraSegmentThreshold;
    }
}
