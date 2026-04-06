package com.flightpathfinder.mcp.pricelookup;

import java.util.List;

/**
 * 最低价比价结果。
 *
 * @param date 查询日期
 * @param requestedPairs 请求的城市对列表
 * @param items 成功匹配的价格条目
 * @param missingPairs 未命中的城市对列表
 */
public record PriceLookupResult(
        String date,
        List<String> requestedPairs,
        List<PriceLookupItem> items,
        List<String> missingPairs) {

    /**
     * 构造时完成字符串规整与集合不可变拷贝。
     */
    public PriceLookupResult {
        date = date == null ? "" : date.trim();
        requestedPairs = List.copyOf(requestedPairs == null ? List.of() : requestedPairs);
        items = List.copyOf(items == null ? List.of() : items);
        missingPairs = List.copyOf(missingPairs == null ? List.of() : missingPairs);
    }
}
