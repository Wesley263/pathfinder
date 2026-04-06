package com.flightpathfinder.mcp.graph.search;

/**
 * 搜索执行配置聚合对象。
 */
final class GraphPathSearchProfile {

    /** frontier 控制策略。 */
    private final GraphPathFrontierPolicy frontierPolicy;
    /** 路径评分配置。 */
    private final GraphPathScoringProfile scoringProfile;

    /**
     * 构造搜索配置。
     */
    private GraphPathSearchProfile(GraphPathFrontierPolicy frontierPolicy,
                                   GraphPathScoringProfile scoringProfile) {
        this.frontierPolicy = frontierPolicy;
        this.scoringProfile = scoringProfile;
    }

    /**
     * 为请求生成默认配置。
     *
     * @param request 搜索请求
     * @return 默认搜索配置
     */
    static GraphPathSearchProfile defaultFor(GraphPathSearchRequest request) {
        return new GraphPathSearchProfile(
                GraphPathFrontierPolicy.defaultPolicy(request),
                GraphPathScoringProfile.defaultProfile());
    }

    /** 返回 frontier 控制策略。 */
    GraphPathFrontierPolicy frontierPolicy() {
        return frontierPolicy;
    }

    /** 返回路径评分配置。 */
    GraphPathScoringProfile scoringProfile() {
        return scoringProfile;
    }
}
