package com.flightpathfinder.mcp.pricelookup;

import java.time.LocalDate;
import java.util.List;

/**
 * 最低价比价查询请求。
 *
 * @param date 出行日期
 * @param cityPairs 待比较城市对集合
 */
public record PriceLookupQuery(
        LocalDate date,
        List<PriceLookupCityPair> cityPairs) {

    /**
     * 构造时完成必填项与集合防御性拷贝校验。
     */
    public PriceLookupQuery {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        cityPairs = List.copyOf(cityPairs == null ? List.of() : cityPairs);
        if (cityPairs.isEmpty()) {
            throw new IllegalArgumentException("at least one city pair is required");
        }
    }
}
