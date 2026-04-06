package com.flightpathfinder.mcp.pricelookup;

import java.util.Locale;

/**
 * 价格比价请求中的城市对。
 *
 * @param origin 出发机场 IATA 三字码
 * @param destination 到达机场 IATA 三字码
 */
public record PriceLookupCityPair(String origin, String destination) {

    /**
     * 构造时将机场代码标准化为去空格大写格式。
     */
    public PriceLookupCityPair {
        origin = normalize(origin);
        destination = normalize(destination);
    }

    /**
     * 生成稳定的城市对键值，格式为 {@code ORG,DST}。
     *
     * @return 城市对字符串键
     */
    public String pairKey() {
        return origin + "," + destination;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
