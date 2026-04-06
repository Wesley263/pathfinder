package com.flightpathfinder.mcp.citycost;

/**
 * 城市成本查询的服务端契约。
 *
 * 定义按城市代码查询成本数据的标准入口。
 */
public interface CityCostService {

    /**
     * 根据请求的机场或城市代码查询城市成本条目。
     *
     * @param query 归一化城市成本查询请求
     * @return 命中成本条目及结构化缺失城市信息
     */
    CityCostResult lookup(CityCostQuery query);
}

