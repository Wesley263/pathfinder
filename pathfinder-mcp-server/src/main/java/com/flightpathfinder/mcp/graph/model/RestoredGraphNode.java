package com.flightpathfinder.mcp.graph.model;

/**
 * 内存图中的节点定义。
 *
 * @param airportCode 机场代码
 * @param airportName 机场名称
 * @param cityName 城市名称
 * @param countryCode 国家代码
 * @param latitude 纬度
 * @param longitude 经度
 * @param timezone 时区
 * @param minTransferMinutes 最短中转分钟数
 */
public record RestoredGraphNode(
        String airportCode,
        String airportName,
        String cityName,
        String countryCode,
        double latitude,
        double longitude,
        String timezone,
        int minTransferMinutes) {
}

