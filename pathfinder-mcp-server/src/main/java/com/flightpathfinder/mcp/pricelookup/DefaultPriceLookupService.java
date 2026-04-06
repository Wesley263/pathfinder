package com.flightpathfinder.mcp.pricelookup;

import com.flightpathfinder.mcp.flightsearch.FlightSearchOption;
import com.flightpathfinder.mcp.flightsearch.FlightSearchQuery;
import com.flightpathfinder.mcp.flightsearch.FlightSearchService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * {@link PriceLookupService} 的默认服务端实现。
 *
 * <p>该服务有意复用 {@link FlightSearchService}，避免重复维护航线 SQL。
 * 同时，价格比价仍保持独立 MCP 契约，因为它需要按城市对汇报覆盖情况，
 * 且结果结构不同于普通直飞查询。
 */
@Service
public class DefaultPriceLookupService implements PriceLookupService {

    private final FlightSearchService flightSearchService;

    public DefaultPriceLookupService(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    /**
     * 为每个请求城市对解析可用最低价选项。
     *
     * @param query 归一化价格查询请求
     * @return 命中价格条目与缺失城市对信息
     */
    @Override
    public PriceLookupResult lookup(PriceLookupQuery query) {
        List<PriceLookupItem> items = new ArrayList<>();
        LinkedHashSet<String> missingPairs = new LinkedHashSet<>();
        LinkedHashSet<String> requestedPairs = new LinkedHashSet<>();

        for (PriceLookupCityPair cityPair : query.cityPairs()) {
            requestedPairs.add(cityPair.pairKey());
            try {
                List<FlightSearchOption> options = flightSearchService.search(
                        new FlightSearchQuery(cityPair.origin(), cityPair.destination(), query.date(), 0, 1));
                // “部分覆盖”是该工具的一等业务结果，
                // 因此记录单个城市对缺失，而不是让整次查询失败。
                if (options.isEmpty()) {
                    missingPairs.add(cityPair.pairKey());
                    continue;
                }
                FlightSearchOption bestOption = options.getFirst();
                items.add(new PriceLookupItem(
                        cityPair.pairKey(),
                        cityPair.origin(),
                        cityPair.destination(),
                        bestOption.airlineCode(),
                        bestOption.airlineName(),
                        bestOption.airlineType(),
                        bestOption.date(),
                        bestOption.priceCny(),
                        bestOption.basePriceCny(),
                        bestOption.durationMinutes(),
                        bestOption.distanceKm(),
                        bestOption.lowCostCarrier()));
            } catch (IllegalArgumentException exception) {
                missingPairs.add(cityPair.pairKey());
            }
        }

        return new PriceLookupResult(
                query.date().toString(),
                List.copyOf(requestedPairs),
                items,
                List.copyOf(missingPairs));
    }
}
