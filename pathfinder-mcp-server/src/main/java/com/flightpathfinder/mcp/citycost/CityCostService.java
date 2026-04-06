package com.flightpathfinder.mcp.citycost;

/**
 * 城市成本查询的服务端契约。
 *
 * <p>MCP 服务端直接拥有该查询路径，因为 {@code city.cost} 读取的是本工具自有数据集，
 * 而不是委托给共享 bootstrap 业务模块。
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
