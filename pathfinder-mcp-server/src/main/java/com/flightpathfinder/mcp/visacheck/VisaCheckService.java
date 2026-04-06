package com.flightpathfinder.mcp.visacheck;

/**
 * 签证政策评估的服务端契约。
 *
 * <p>MCP 服务端在本地执行该规则评估，
 * 使 {@code visa.check} 可直接查询签证政策并应用业务规则，
 * 无需依赖 bootstrap 侧服务。
 */
public interface VisaCheckService {

    /**
     * 针对请求目的地与护照上下文评估签证要求。
     *
     * @param query 归一化签证核验请求
     * @return 结构化签证结果，包含业务级数据缺失状态
     */
    VisaCheckResult check(VisaCheckQuery query);
}
