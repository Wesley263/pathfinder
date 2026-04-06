package com.flightpathfinder.mcp.riskevaluate;

/**
 * 中转风险评估的服务端契约。
 *
 * <p>风险工具保留在 MCP 服务端，
 * 因为它需要同时执行数据源查询与本地规则评分；
 * 引导侧应用仅依赖 MCP 契约，不依赖实现细节。
 */
public interface RiskEvaluateService {

    /**
     * 对查询描述的中转场景进行风险评分。
     *
     * @param query 归一化风险评估请求
     * @return 包含风险等级、解释与建议的结构化结果
     */
    RiskEvaluateResult evaluate(RiskEvaluateQuery query);
}
