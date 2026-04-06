package com.flightpathfinder.mcp.flightsearch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 * 说明。
 */
@Service
public class JdbcFlightSearchService implements FlightSearchService {

    private static final String AIRPORT_EXISTS_SQL = """
            SELECT COUNT(1)
            FROM t_airport
            WHERE deleted = 0
              AND iata_code = ?
            """;

    private static final String ROUTE_SQL = """
            SELECT r.id,
                   r.airline_iata,
                   r.source_airport_iata,
                   r.dest_airport_iata,
                   r.base_price_cny,
                   r.duration_minutes,
                   r.distance_km,
                   a.name AS airline_name,
                   a.airline_type
            FROM t_route r
            LEFT JOIN t_airline a
              ON a.iata_code = r.airline_iata
             AND a.deleted = 0
            WHERE r.deleted = 0
              AND r.source_airport_iata = ?
              AND r.dest_airport_iata = ?
            ORDER BY COALESCE(r.base_price_cny, 999999),
                     COALESCE(r.duration_minutes, 999999),
                     r.id
            LIMIT 40
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcFlightSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询请求机场对的直飞航线，并扩展到日期窗口。
     *
     * @param query 已校验的直飞查询请求
     * @return 返回结果。
     */
    @Override
    public List<FlightSearchOption> search(FlightSearchQuery query) {
        validateAirport(query.origin(), "origin");
        validateAirport(query.destination(), "destination");

        List<RouteRow> routes = jdbcTemplate.query(ROUTE_SQL, new RouteRowMapper(), query.origin(), query.destination());
        if (routes.isEmpty()) {
            return List.of();
        }

        List<LocalDate> travelDates = buildDateWindow(query.date(), query.flexibilityDays());
        List<FlightSearchOption> options = new ArrayList<>();
        for (RouteRow route : routes) {
            for (LocalDate travelDate : travelDates) {
                options.add(toFlightOption(route, travelDate));
            }
        }

        return options.stream()
                .sorted(Comparator.comparingDouble(FlightSearchOption::priceCny)
                        .thenComparingInt(FlightSearchOption::durationMinutes)
                        .thenComparing(FlightSearchOption::airlineCode))
                .limit(Math.max(1, query.topK()))
                .toList();
    }

    private void validateAirport(String iataCode, String role) {
        Integer count = jdbcTemplate.queryForObject(AIRPORT_EXISTS_SQL, Integer.class, iataCode);
        if (count == null || count <= 0) {
            throw new IllegalArgumentException(role + " airport not found: " + iataCode);
        }
    }

    private FlightSearchOption toFlightOption(RouteRow route, LocalDate travelDate) {
        String airlineType = normalizeAirlineType(route.airlineType(), route.airlineCode());
        boolean lowCostCarrier = "LCC".equals(airlineType);
        double basePrice = route.basePriceCny() > 0D ? route.basePriceCny() : estimateBasePrice(route.distanceKm());
        // 这里有意采用启发式定价：即使数据集只有航线基准价而非实时票价，
        // 工具仍需给出稳定可诊断的检索估算结果。
        double dynamicPrice = applyPricing(basePrice, airlineType, route.distanceKm(), travelDate, route.routeId());
        int durationMinutes = route.durationMinutes() > 0 ? route.durationMinutes() : estimateDuration(route.distanceKm());

        return new FlightSearchOption(
                route.airlineCode(),
                route.airlineName(),
                airlineType,
                route.origin(),
                route.destination(),
                travelDate.toString(),
                round(dynamicPrice),
                round(basePrice),
                durationMinutes,
                round(route.distanceKm()),
                lowCostCarrier);
    }

    private List<LocalDate> buildDateWindow(LocalDate centerDate, int flexibilityDays) {
        List<LocalDate> dates = new ArrayList<>();
        for (int offset = -flexibilityDays; offset <= flexibilityDays; offset++) {
            dates.add(centerDate.plusDays(offset));
        }
        return dates;
    }

    private double applyPricing(double basePrice,
                                String airlineType,
                                double distanceKm,
                                LocalDate travelDate,
                                long routeId) {
        double airlineFactor = switch (airlineType) {
            case "LCC" -> 0.88D;
            case "FSC" -> 1.08D;
            default -> 1.00D;
        };
        double dateFactor = isWeekend(travelDate) ? 1.12D : 0.96D;
        double distanceTax = distanceKm >= 7000D ? 320D : distanceKm >= 2500D ? 180D : 80D;
        double hashFactor = hashFactor(travelDate, routeId);
        return Math.max(150D, (basePrice * airlineFactor * dateFactor + distanceTax) * hashFactor);
    }

    private boolean isWeekend(LocalDate travelDate) {
        DayOfWeek dayOfWeek = travelDate.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private double hashFactor(LocalDate travelDate, long routeId) {
        int hash = (travelDate + ":" + routeId).hashCode();
        double variation = (Math.abs(hash) % 101 - 50) / 1000.0D;
        return 1.0D + variation;
    }

    private double estimateBasePrice(double distanceKm) {
        double safeDistance = Math.max(distanceKm, 400D);
        return 180D + Math.pow(safeDistance, 0.78D) * 1.65D;
    }

    private int estimateDuration(double distanceKm) {
        double safeDistance = Math.max(distanceKm, 500D);
        return Math.max(60, (int) Math.round(safeDistance / 760D * 60D));
    }

    private String normalizeAirlineType(String airlineType, String airlineCode) {
        String normalized = airlineType == null ? "" : airlineType.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            return normalized;
        }
        // 当航司元数据不完整时，回退策略可保持检索可用。
        // 这在来源于第三方航线数据的导入场景中较常见。
        return switch (airlineCode) {
            case "9C", "AK", "QZ", "FD", "TR", "FR", "W6", "U2" -> "LCC";
            case "CA", "MU", "CZ", "NH", "JL", "SQ", "LH", "BA", "AF" -> "FSC";
            default -> "MID";
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static final class RouteRowMapper implements RowMapper<RouteRow> {

        @Override
        public RouteRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RouteRow(
                    rs.getLong("id"),
                    firstNonBlank(rs.getString("airline_iata"), "UNKNOWN"),
                    firstNonBlank(rs.getString("airline_name"), rs.getString("airline_iata"), "UNKNOWN"),
                    rs.getString("airline_type"),
                    rs.getString("source_airport_iata"),
                    rs.getString("dest_airport_iata"),
                    getDouble(rs, "base_price_cny"),
                    getInt(rs, "duration_minutes"),
                    getDouble(rs, "distance_km"));
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static double getDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? 0.0D : value;
    }

    private static int getInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? 0 : value;
    }

    private record RouteRow(
            long routeId,
            String airlineCode,
            String airlineName,
            String airlineType,
            String origin,
            String destination,
            double basePriceCny,
            int durationMinutes,
            double distanceKm) {
    }
}
