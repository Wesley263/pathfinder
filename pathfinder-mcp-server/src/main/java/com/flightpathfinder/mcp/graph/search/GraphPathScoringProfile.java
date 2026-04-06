package com.flightpathfinder.mcp.graph.search;

/**
 * 路径候选评分配置。
 */
final class GraphPathScoringProfile {

    /** 价格权重。 */
    private final double priceWeight;
    /** 时长权重。 */
    private final double durationWeight;
    /** 准点率权重。 */
    private final double reliabilityWeight;
    /** 中转次数权重。 */
    private final double transferWeight;
    /** 经停次数权重。 */
    private final double stopWeight;
    /** 行李权重。 */
    private final double baggageWeight;
    /** 绕路效率权重。 */
    private final double detourWeight;
    /** 竞争度权重。 */
    private final double competitionWeight;
    /** 注释说明。 */
    private final double paretoLayerWeight;
    /** 绕路参考比值。 */
    private final double detourReferenceRatio;
    /** 竞争度参考值。 */
    private final double competitionReference;

    /**
     * 构造评分配置。
     */
    private GraphPathScoringProfile(double priceWeight,
                                    double durationWeight,
                                    double reliabilityWeight,
                                    double transferWeight,
                                    double stopWeight,
                                    double baggageWeight,
                                    double detourWeight,
                                    double competitionWeight,
                                    double paretoLayerWeight,
                                    double detourReferenceRatio,
                                    double competitionReference) {
        this.priceWeight = priceWeight;
        this.durationWeight = durationWeight;
        this.reliabilityWeight = reliabilityWeight;
        this.transferWeight = transferWeight;
        this.stopWeight = stopWeight;
        this.baggageWeight = baggageWeight;
        this.detourWeight = detourWeight;
        this.competitionWeight = competitionWeight;
        this.paretoLayerWeight = paretoLayerWeight;
        this.detourReferenceRatio = detourReferenceRatio;
        this.competitionReference = competitionReference;
    }

    /**
     * 返回默认评分配置。
     *
     * @return 默认配置
     */
    static GraphPathScoringProfile defaultProfile() {
        return new GraphPathScoringProfile(
                0.28,
                0.23,
                0.18,
                0.11,
                0.05,
                0.05,
                0.06,
                0.02,
                0.02,
                2.20,
                8.0);
    }

    /** 注释说明。 */
    double priceWeight() {
        return priceWeight;
    }

    /** 注释说明。 */
    double durationWeight() {
        return durationWeight;
    }

    /** 注释说明。 */
    double reliabilityWeight() {
        return reliabilityWeight;
    }

    /** 注释说明。 */
    double transferWeight() {
        return transferWeight;
    }

    /** 注释说明。 */
    double stopWeight() {
        return stopWeight;
    }

    /** 注释说明。 */
    double baggageWeight() {
        return baggageWeight;
    }

    /** 注释说明。 */
    double detourWeight() {
        return detourWeight;
    }

    /** 注释说明。 */
    double competitionWeight() {
        return competitionWeight;
    }

    /** 注释说明。 */
    double paretoLayerWeight() {
        return paretoLayerWeight;
    }

    /** 注释说明。 */
    double detourReferenceRatio() {
        return detourReferenceRatio;
    }

    /** 注释说明。 */
    double competitionReference() {
        return competitionReference;
    }
}
