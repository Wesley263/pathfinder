package com.flightpathfinder.mcp.riskevaluate;

import java.util.List;

/**
 * 中转风险评估结果。
 *
 * @param hubAirport 中转枢纽机场
 * @param firstAirline 第一段航司代码
 * @param secondAirline 第二段航司代码
 * @param bufferHours 参与评估的缓冲时长（小时）
 * @param riskLevel 风险等级
 * @param riskScore 综合风险分数
 * @param airlineRiskScore 航司维度风险分数
 * @param bufferRiskScore 缓冲时长维度风险分数
 * @param hubRiskScore 枢纽维度风险分数
 * @param suggestedBufferHours 建议缓冲时长（小时）
 * @param explanation 风险解释文本
 * @param recommendations 风险处置建议
 * @param sameAirline 是否同航司中转
 * @param dataComplete 参考数据是否完整
 * @param missingInputs 缺失输入或缺失参考项
 */
public record RiskEvaluateResult(
        String hubAirport,
        String firstAirline,
        String secondAirline,
        double bufferHours,
        String riskLevel,
        double riskScore,
        double airlineRiskScore,
        double bufferRiskScore,
        double hubRiskScore,
        double suggestedBufferHours,
        String explanation,
        List<String> recommendations,
        boolean sameAirline,
        boolean dataComplete,
        List<String> missingInputs) {
}
