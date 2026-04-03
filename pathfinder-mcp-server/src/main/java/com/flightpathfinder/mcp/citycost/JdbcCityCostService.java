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
 * JDBC-backed implementation of {@link CityCostService}.
 *
 * <p>This service keeps {@code city.cost} independent from bootstrap business modules by reading cost data
 * directly from the MCP server datasource and returning a management-friendly partial-coverage result.
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
     * Looks up cost data for each requested code.
     *
     * @param query normalized city-cost request
     * @return matched cost entries sorted by daily cost, plus missing-city information
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
                // Missing cities are preserved as a business fact so callers can distinguish partial coverage
                // from a complete infrastructure failure.
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
