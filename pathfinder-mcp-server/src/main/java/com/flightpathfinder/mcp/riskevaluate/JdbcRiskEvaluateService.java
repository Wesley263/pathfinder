package com.flightpathfinder.mcp.riskevaluate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * JDBC-backed implementation of {@link RiskEvaluateService}.
 *
 * <p>This service owns the current transfer-risk rule model for {@code risk.evaluate}. It reads reference
 * data locally and applies server-side scoring so the tool can evolve without sharing bootstrap business
 * implementations.
 */
@Service
public class JdbcRiskEvaluateService implements RiskEvaluateService {

    private static final double W_AIRLINE = 0.35D;
    private static final double W_BUFFER = 0.40D;
    private static final double W_HUB = 0.25D;
    private static final double CROSS_AIRLINE_PENALTY = 0.10D;
    private static final double THRESHOLD_LOW = 0.25D;
    private static final double THRESHOLD_HIGH = 0.55D;

    private static final Set<String> MAJOR_HUBS = Set.of(
            "NRT", "HND", "ICN", "SIN", "HKG", "PVG", "PEK",
            "DXB", "DOH", "IST", "LHR", "CDG", "FRA", "AMS",
            "JFK", "LAX", "ORD", "ATL", "SFO", "SYD", "BKK");

    private static final Set<String> MEDIUM_HUBS = Set.of(
            "KIX", "TPE", "KUL", "CGK", "BOM", "DEL", "MNL",
            "FCO", "MAD", "MUC", "ZRH", "VIE", "YYZ", "SEA",
            "MEL", "AKL", "CAN", "CTU", "SHA", "XIY", "SZX");

    private static final String AIRPORT_EXISTS_SQL = """
            SELECT COUNT(1)
            FROM t_airport
            WHERE deleted = 0
              AND iata_code = ?
            """;

    private static final String AIRLINE_SQL = """
            SELECT iata_code,
                   name,
                   airline_type
            FROM t_airline
            WHERE deleted = 0
              AND iata_code = ?
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcRiskEvaluateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Evaluates a transfer scenario using hub, airline and buffer heuristics.
     *
     * @param query normalized risk-evaluation request
     * @return structured risk result including score, level, explanation and recommendations
     */
    @Override
    public RiskEvaluateResult evaluate(RiskEvaluateQuery query) {
        LinkedHashSet<String> missingInputs = new LinkedHashSet<>();
        if (!hubAirportExists(query.hubAirport())) {
            missingInputs.add("hubAirport");
        }

        Optional<AirlineProfile> firstAirline = findAirline(query.firstAirline());
        Optional<AirlineProfile> secondAirline = findAirline(query.secondAirline());
        if (firstAirline.isEmpty()) {
            missingInputs.add("firstAirline");
        }
        if (secondAirline.isEmpty()) {
            missingInputs.add("secondAirline");
        }

        if (!missingInputs.isEmpty()) {
            return dataNotFound(query, missingInputs);
        }

        AirlineProfile firstProfile = firstAirline.orElseThrow();
        AirlineProfile secondProfile = secondAirline.orElseThrow();
        double firstOnTimeRate = onTimeRate(firstProfile.airlineType());
        double secondOnTimeRate = onTimeRate(secondProfile.airlineType());
        boolean sameAirline = query.firstAirline().equals(query.secondAirline());

        double airlineRisk = ((1 - firstOnTimeRate) + (1 - secondOnTimeRate)) / 2.0D;
        if (!sameAirline) {
            // Cross-airline connections are intentionally penalized because recheck and baggage handling add a
            // consistent operational risk even before schedule reliability is considered.
            airlineRisk += CROSS_AIRLINE_PENALTY;
        }
        airlineRisk = clamp(airlineRisk);

        double bufferRisk = computeBufferRisk(query.bufferHours());
        double hubRisk = computeHubRisk(query.hubAirport());
        double riskScore = W_AIRLINE * airlineRisk + W_BUFFER * bufferRisk + W_HUB * hubRisk;

        String riskLevel;
        double suggestedBufferHours;
        if (riskScore < THRESHOLD_LOW) {
            riskLevel = "LOW";
            suggestedBufferHours = 1.5D;
        } else if (riskScore > THRESHOLD_HIGH) {
            riskLevel = "HIGH";
            suggestedBufferHours = 4.0D;
        } else {
            riskLevel = "MEDIUM";
            suggestedBufferHours = 2.5D;
        }

        String explanation = buildExplanation(
                riskLevel,
                query.hubAirport(),
                query.firstAirline(),
                query.secondAirline(),
                query.bufferHours(),
                suggestedBufferHours,
                sameAirline);

        List<String> recommendations = buildRecommendations(
                query.hubAirport(),
                query.bufferHours(),
                suggestedBufferHours,
                sameAirline,
                airlineRisk,
                hubRisk,
                riskLevel);

        return new RiskEvaluateResult(
                query.hubAirport(),
                query.firstAirline(),
                query.secondAirline(),
                round(query.bufferHours()),
                riskLevel,
                round(riskScore),
                round(airlineRisk),
                round(bufferRisk),
                round(hubRisk),
                round(suggestedBufferHours),
                explanation,
                recommendations,
                sameAirline,
                true,
                List.of());
    }

    private RiskEvaluateResult dataNotFound(RiskEvaluateQuery query, Set<String> missingInputs) {
        List<String> recommendations = List.of(
                "Confirm the hub airport IATA code and both airline codes.",
                "Retry after providing a supported hub and airline combination.");
        return new RiskEvaluateResult(
                query.hubAirport(),
                query.firstAirline(),
                query.secondAirline(),
                round(query.bufferHours()),
                "DATA_NOT_FOUND",
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                "Risk evaluation could not be completed because hub or airline reference data is missing.",
                recommendations,
                query.firstAirline().equals(query.secondAirline()),
                false,
                List.copyOf(missingInputs));
    }

    private boolean hubAirportExists(String hubAirport) {
        Integer count = jdbcTemplate.queryForObject(AIRPORT_EXISTS_SQL, Integer.class, hubAirport);
        return count != null && count > 0;
    }

    private Optional<AirlineProfile> findAirline(String airlineCode) {
        List<AirlineProfile> rows = jdbcTemplate.query(AIRLINE_SQL, new AirlineProfileRowMapper(), airlineCode);
        return rows.stream().findFirst();
    }

    private double onTimeRate(String airlineType) {
        return switch (normalizeAirlineType(airlineType)) {
            case "FSC" -> 0.88D;
            case "MID" -> 0.82D;
            case "LCC" -> 0.75D;
            default -> 0.80D;
        };
    }

    private double computeBufferRisk(double bufferHours) {
        // Buffer scoring is intentionally coarse-grained in v1 so results stay explainable to operators.
        if (bufferHours < 1.5D) {
            return 1.0D;
        }
        if (bufferHours < 3.0D) {
            return 0.40D;
        }
        if (bufferHours < 5.0D) {
            return 0.15D;
        }
        return 0.05D;
    }

    private double computeHubRisk(String hubAirport) {
        // Hub classification is kept local to the server-side rule model until a richer operational dataset is
        // introduced.
        if (MAJOR_HUBS.contains(hubAirport)) {
            return 0.10D;
        }
        if (MEDIUM_HUBS.contains(hubAirport)) {
            return 0.30D;
        }
        return 0.60D;
    }

    private String buildExplanation(String riskLevel,
                                    String hubAirport,
                                    String firstAirline,
                                    String secondAirline,
                                    double actualBuffer,
                                    double suggestedBuffer,
                                    boolean sameAirline) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transfer risk at ").append(hubAirport).append(" is ").append(riskLevel).append(". ");
        if (!sameAirline) {
            sb.append("Different airlines (")
                    .append(firstAirline)
                    .append(" to ")
                    .append(secondAirline)
                    .append(") increase connection and baggage-transfer uncertainty. ");
        }
        if (actualBuffer < suggestedBuffer) {
            sb.append("Current buffer ")
                    .append(round(actualBuffer))
                    .append("h is below the suggested ")
                    .append(round(suggestedBuffer))
                    .append("h.");
        } else {
            sb.append("Current buffer ")
                    .append(round(actualBuffer))
                    .append("h is within the suggested safety window.");
        }
        return sb.toString();
    }

    private List<String> buildRecommendations(String hubAirport,
                                              double actualBuffer,
                                              double suggestedBuffer,
                                              boolean sameAirline,
                                              double airlineRisk,
                                              double hubRisk,
                                              String riskLevel) {
        List<String> recommendations = new ArrayList<>();
        if (actualBuffer < suggestedBuffer) {
            recommendations.add("Increase transfer buffer to at least " + round(suggestedBuffer) + " hours.");
        }
        if (!sameAirline) {
            recommendations.add("Prefer a same-airline connection to reduce recheck and baggage-transfer risk.");
        }
        if (airlineRisk >= 0.25D) {
            recommendations.add("Prefer a more punctual carrier if there is an equivalent option.");
        }
        if (hubRisk >= 0.60D) {
            recommendations.add("Consider a higher-efficiency hub if your schedule is flexible.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add(switch (riskLevel) {
                case "LOW" -> "Current connection risk looks acceptable under the available data.";
                case "MEDIUM" -> "Keep monitoring schedule changes and allow some extra margin on the day of travel.";
                default -> "Review this connection carefully before booking or departing.";
            });
        }
        return List.copyOf(recommendations);
    }

    private String normalizeAirlineType(String airlineType) {
        String normalized = airlineType == null ? "" : airlineType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "FSC", "MID", "LCC" -> normalized;
            default -> "MID";
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static final class AirlineProfileRowMapper implements RowMapper<AirlineProfile> {

        @Override
        public AirlineProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AirlineProfile(
                    rs.getString("iata_code"),
                    rs.getString("name"),
                    rs.getString("airline_type"));
        }
    }

    private record AirlineProfile(String iataCode, String name, String airlineType) {
    }
}
