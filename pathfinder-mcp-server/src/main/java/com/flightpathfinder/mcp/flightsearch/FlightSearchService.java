package com.flightpathfinder.mcp.flightsearch;

import java.util.List;

/**
 * 直飞检索的服务端契约。
 *
 * 定义面向机场对和日期窗口的直飞检索能力。
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

