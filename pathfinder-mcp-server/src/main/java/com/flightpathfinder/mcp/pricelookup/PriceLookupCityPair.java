package com.flightpathfinder.mcp.pricelookup;

import java.util.Locale;

/**
 * 价格比价请求中的城市对。
 *
 * @param origin 参数说明。
 * @param destination 参数说明。
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
     * 说明。
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
