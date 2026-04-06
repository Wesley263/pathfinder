package com.flightpathfinder.framework.readmodel.graph;

import java.util.Map;

/**
 * 图快照中的节点定义。
 *
 * @param nodeId 节点唯一标识
 * @param airportCode 机场代码
 * @param airportName 机场名称
 * @param cityName 城市名称
 * @param countryCode 国家代码
 * @param latitude 纬度
 * @param longitude 经度
 * @param timezone 时区
 * @param visaFreeCn 是否对中国护照免签
 * @param transitVisaFree 是否支持过境免签
 * @param dailyCostUsd 日均成本（美元）
 * @param minTransferMinutes 最短中转分钟数
 * @param attributes 扩展属性
 */
public record GraphSnapshotNode(
        String nodeId,
        String airportCode,
        String airportName,
        String cityName,
        String countryCode,
        double latitude,
        double longitude,
        String timezone,
        boolean visaFreeCn,
        boolean transitVisaFree,
        double dailyCostUsd,
        int minTransferMinutes,
        Map<String, Object> attributes) {
}
