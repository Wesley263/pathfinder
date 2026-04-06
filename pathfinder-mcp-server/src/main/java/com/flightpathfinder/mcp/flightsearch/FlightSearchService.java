package com.flightpathfinder.mcp.flightsearch;

import java.util.List;

/**
 * 直飞检索的服务端契约。
 *
 * <p>该服务驻留在 {@code pathfinder-mcp-server}，
 * 因为 {@code flight.search} 是基于服务端自有数据源访问的独立 MCP 工具，
 * 不依赖共享的 bootstrap 业务实现。
 */
public interface FlightSearchService {

    /**
     * 查询给定机场对与日期窗口下的直飞选项。
     *
     * @param query 归一化后的直飞查询请求
     * @return 排序后的直飞选项；若无匹配则返回空列表
     */
    List<FlightSearchOption> search(FlightSearchQuery query);
}
