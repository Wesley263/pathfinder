package com.flightpathfinder.mcp.citycost;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * {@link CityCostService} 的 JDBC 实现。
 *
 * <p>该服务通过直接读取 MCP 服务端数据源中的城市成本数据，
 * 让 {@code city.cost} 保持对 bootstrap 业务模块的独立性，
 * 并返回便于管理侧消费的部分覆盖结果。
 */
@Service
public class JdbcCityCostService implements CityCostService {

    private static final String CITY_COST_SQL = """
            SELECT iata_code,
                   city,
                   country,
                   country_code,
                   daily_cost_usd,
                   accommodation_usd,
                   meal_cost_usd,
                   transportation_usd
            FROM t_city_cost
            WHERE deleted = 0
              AND iata_code = ?
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCityCostService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 为每个请求代码查询成本数据。
     *
     * @param query 归一化城市成本查询请求
     * @return 按日成本排序的命中条目，以及缺失城市信息
     */
    @Override
    public CityCostResult lookup(CityCostQuery query) {
        List<CityCostItem> items = new ArrayList<>();
        LinkedHashSet<String> missingCities = new LinkedHashSet<>();
        for (String iataCode : query.iataCodes()) {
            Optional<CityCostItem> cityCostItem = findByIataCode(iataCode);
            if (cityCostItem.isPresent()) {
                items.add(cityCostItem.get());
            } else {
                // 缺失城市会被保留为业务事实，
                // 便于调用方区分“部分覆盖”与“基础设施整体故障”。
                missingCities.add(iataCode);
            }
        }

        List<CityCostItem> sortedItems = items.stream()
                .sorted(Comparator.comparingDouble(CityCostItem::dailyCostUsd)
                        .thenComparing(CityCostItem::iataCode))
                .toList();

        return new CityCostResult(
                query.iataCodes(),
                sortedItems,
                List.copyOf(missingCities));
    }

    private Optional<CityCostItem> findByIataCode(String iataCode) {
        List<CityCostItem> rows = jdbcTemplate.query(CITY_COST_SQL, new CityCostRowMapper(), iataCode);
        return rows.stream().findFirst();
    }

    private static final class CityCostRowMapper implements RowMapper<CityCostItem> {

        @Override
        public CityCostItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CityCostItem(
                    rs.getString("iata_code"),
                    rs.getString("city"),
                    rs.getString("country"),
                    rs.getString("country_code"),
                    getDouble(rs, "daily_cost_usd"),
                    getDouble(rs, "accommodation_usd"),
                    getDouble(rs, "meal_cost_usd"),
                    getDouble(rs, "transportation_usd"));
        }
    }

    private static double getDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? 0.0D : value;
    }
}
