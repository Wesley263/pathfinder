package com.flightpathfinder.mcp.visacheck;

/**
 * 签证政策评估的服务端契约。
 *
 * 定义按目的地国家与护照国家执行签证核验的标准入口。
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

