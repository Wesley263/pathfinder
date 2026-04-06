package com.flightpathfinder.mcp.pricelookup;

/**
 * 多城市对最低价比较的服务端契约。
 *
 * <p>尽管内部复用 {@code flight.search} 能力，该服务仍留在服务端，
 * 因为对外 MCP 工具拥有独立输入契约与结果语义。
 */
public interface PriceLookupService {

    /**
     * 为每个请求城市对解析可用最低价方案。
     *
     * @param query 归一化查询请求
     * @return 结构化结果，包含命中城市对与缺失覆盖信息
     */
    PriceLookupResult lookup(PriceLookupQuery query);
}
