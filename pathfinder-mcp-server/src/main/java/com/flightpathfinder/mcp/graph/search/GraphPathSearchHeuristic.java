package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphNode;

/**
 * 图路径搜索启发式评分助手。
 *
 * 负责对部分状态与完成候选路径进行多维归一化评分，
 * 并提供绕路裁剪等搜索期判定能力。
 */
final class GraphPathSearchHeuristic {

    /** 恢复图。 */
    private final RestoredFlightGraph graph;
    /** 搜索请求。 */
    private final GraphPathSearchRequest request;
    /** 预计算下界。 */
    private final GraphPathSearchLowerBounds lowerBounds;
    /** frontier 控制策略。 */
    private final GraphPathFrontierPolicy frontierPolicy;
    /** 评分配置。 */
    private final GraphPathScoringProfile scoringProfile;
    /** 价格参考值。 */
    private final double priceReferenceCny;
    /** 时长参考值。 */
    private final double durationReferenceMinutes;
    /** 中转参考值。 */
    private final double transferReference;
    /** 经停参考值。 */
    private final double stopReference;

    /**
     * 构造启发式助手。
     */
    GraphPathSearchHeuristic(RestoredFlightGraph graph,
                             GraphPathSearchRequest request,
                             GraphPathSearchLowerBounds lowerBounds,
                             GraphPathSearchProfile profile) {
        this.graph = graph;
        this.request = request;
        this.lowerBounds = lowerBounds;
        this.frontierPolicy = profile.frontierPolicy();
        this.scoringProfile = profile.scoringProfile();
        double originMinPrice = finiteDouble(lowerBounds.minRemainingPriceCny(request.origin()), 1200.0);
        int originMinDuration = finiteInt(lowerBounds.minRemainingDurationMinutes(request.origin()), 360);
        this.priceReferenceCny = Math.max(originMinPrice, Math.min(request.maxBudget(), originMinPrice * 3.0));
        int transferPadding = Math.max(0, request.maxSegments() - 1) * (120 + request.stopoverDays() * 24 * 60);
        this.durationReferenceMinutes = Math.max(originMinDuration + transferPadding, originMinDuration * 1.8);
        this.transferReference = Math.max(1, request.maxSegments() - 1);
        this.stopReference = Math.max(1, request.maxSegments() * 2);
    }

    /**
     * 基于乐观下界对部分状态打分。
     */
    double optimisticScore(GraphPathPartialState state) {
        int remainingSegments = lowerBounds.minRemainingSegments(state.currentAirport());
        double estimatedPrice = state.totalPriceCny() + finiteDouble(lowerBounds.minRemainingPriceCny(state.currentAirport()), priceReferenceCny);
        int estimatedDuration = state.totalDurationMinutes() + finiteInt(lowerBounds.minRemainingDurationMinutes(state.currentAirport()), (int) durationReferenceMinutes);
        int estimatedTransfers = Math.max(0, state.segmentsUsed() + remainingSegments - 1);
        double optimisticReliability = optimisticReliability(state, remainingSegments);
        double optimisticCompetition = normalizeUpper(state.averageCompetitionCount() == 0.0 ? scoringProfile.competitionReference() * 0.5 : state.averageCompetitionCount(),
                scoringProfile.competitionReference());
        return composeScore(
                normalizeLower(estimatedPrice, priceReferenceCny),
                normalizeLower(estimatedDuration, durationReferenceMinutes),
                optimisticReliability,
                normalizeLower(estimatedTransfers, transferReference),
                normalizeLower(state.totalStops(), stopReference),
                state.baggageIncluded() ? 1.0 : 0.0,
                detourUtility(lowerBounds.estimatedDetourRatio(state.currentAirport(), state.totalDistanceKm())),
                optimisticCompetition);
    }

    /**
     * 对完成候选路径打分，用于候选池准入。
     */
    double candidateScore(RestoredCandidatePath candidate) {
        return composeScore(
                normalizeLower(candidate.totalPriceCny(), priceReferenceCny),
                normalizeLower(candidate.totalDurationMinutes(), durationReferenceMinutes),
                candidate.averageOnTimeRate(),
                normalizeLower(candidate.transferCount(), transferReference),
                normalizeLower(candidate.totalStops(), stopReference),
                candidate.baggageIncluded() ? 1.0 : 0.0,
                detourUtility(actualDetourRatio(candidate)),
                normalizeUpper(candidate.averageCompetitionCount(), scoringProfile.competitionReference()));
    }

    /**
     * 过滤估计绕路过大的部分状态。
     */
    boolean shouldPruneByDetour(GraphPathPartialState state) {
        if (state.segmentsUsed() < 2) {
            return false;
        }
        // 绕路裁剪在路径形态足够成型后才启用，避免早期航段被过早惩罚。
        double threshold = frontierPolicy.detourBaseThreshold()
                + Math.max(0, request.maxSegments() - 2) * frontierPolicy.detourPerExtraSegmentThreshold();
        return lowerBounds.estimatedDetourRatio(state.currentAirport(), state.totalDistanceKm()) > threshold;
    }

    /**
     * 组合各维度分值为最终分。
     */
    private double composeScore(double priceUtility,
                                double durationUtility,
                                double reliabilityUtility,
                                double transferUtility,
                                double stopUtility,
                                double baggageUtility,
                                double detourUtility,
                                double competitionUtility) {
        return priceUtility * scoringProfile.priceWeight()
                + durationUtility * scoringProfile.durationWeight()
                + reliabilityUtility * scoringProfile.reliabilityWeight()
                + transferUtility * scoringProfile.transferWeight()
                + stopUtility * scoringProfile.stopWeight()
                + baggageUtility * scoringProfile.baggageWeight()
                + detourUtility * scoringProfile.detourWeight()
                + competitionUtility * scoringProfile.competitionWeight();
    }

    /**
     * 估计乐观准点率。
     */
    private double optimisticReliability(GraphPathPartialState state, int remainingSegments) {
        if (state.segmentsUsed() == 0 && remainingSegments == 0) {
            return 1.0;
        }
        double completed = state.averageOnTimeRate() * state.segmentsUsed();
        double bestPossible = completed + Math.max(0, remainingSegments);
        double totalSegments = Math.max(1, state.segmentsUsed() + Math.max(0, remainingSegments));
        return Math.max(0.0, Math.min(1.0, bestPossible / totalSegments));
    }

    /**
     * 计算实际候选路径的绕路比。
     */
    private double actualDetourRatio(RestoredCandidatePath candidate) {
        if (candidate.legs().isEmpty()) {
            return 1.0;
        }
        RestoredGraphNode origin = graph.getNode(candidate.legs().get(0).origin());
        RestoredGraphNode destination = graph.getNode(candidate.legs().get(candidate.legs().size() - 1).destination());
        if (origin == null || destination == null || candidate.totalDistanceKm() <= 0.0) {
            return 1.0;
        }
        double directDistance = haversineKm(
                origin.latitude(),
                origin.longitude(),
                destination.latitude(),
                destination.longitude());
        if (directDistance <= 0.0) {
            return 1.0;
        }
        return candidate.totalDistanceKm() / directDistance;
    }

    /**
     * 将绕路比转换为效用分。
     */
    private double detourUtility(double detourRatio) {
        if (detourRatio <= 1.0) {
            return 1.0;
        }
        double overage = detourRatio - 1.0;
        return Math.max(0.0, 1.0 - (overage / Math.max(0.1, scoringProfile.detourReferenceRatio() - 1.0)));
    }

    /**
     * 低值更优归一化。
     */
    private double normalizeLower(double value, double reference) {
        if (reference <= 0.0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - Math.min(1.0, value / reference));
    }

    /**
     * 高值更优归一化。
     */
    private double normalizeUpper(double value, double reference) {
        if (reference <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value / reference));
    }

    /**
     * 将数值归一到有限范围，不可用时返回兜底值。
     */
    private double finiteDouble(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    /**
     * 将整数下界转换为可用值，不可用时返回兜底值。
     */
    private int finiteInt(int value, int fallback) {
        return value < Integer.MAX_VALUE ? value : fallback;
    }

    /**
     * 计算两地大圆距离（公里）。
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
