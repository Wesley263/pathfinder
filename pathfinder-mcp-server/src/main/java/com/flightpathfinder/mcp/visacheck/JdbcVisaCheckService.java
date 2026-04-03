package com.flightpathfinder.mcp.visacheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * JDBC-backed implementation of {@link VisaCheckService}.
 *
 * <p>This service keeps visa-policy lookup and rule interpretation inside {@code pathfinder-mcp-server} so
 * {@code visa.check} can stay an independent owner tool backed by server-local data.
 */
@Service
public class JdbcVisaCheckService implements VisaCheckService {

    private static final String VISA_POLICY_SQL = """
            SELECT destination_country,
                   passport_country,
                   visa_required,
                   visa_duration_days,
                   transit_conditions,
                   notes
            FROM t_visa_policy
            WHERE deleted = 0
              AND destination_country = ?
              AND passport_country = ?
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcVisaCheckService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluates visa policy outcomes for all requested destination countries.
     *
     * @param query normalized visa-check request
     * @return per-country visa outcomes including structured missing-data items
     */
    @Override
    public VisaCheckResult check(VisaCheckQuery query) {
        List<VisaCheckItem> items = new ArrayList<>();
        for (String countryCode : query.countryCodes()) {
            Optional<VisaPolicyRow> policyRow = findPolicy(countryCode, query.passportCountry());
            items.add(policyRow
                    .map(row -> evaluate(countryCode, query, row))
                    .orElseGet(() -> missingData(countryCode, query.passportCountry())));
        }
        return new VisaCheckResult(query.passportCountry(), query.stayDays(), items);
    }

    private Optional<VisaPolicyRow> findPolicy(String destinationCountry, String passportCountry) {
        List<VisaPolicyRow> rows = jdbcTemplate.query(VISA_POLICY_SQL, new VisaPolicyRowMapper(), destinationCountry, passportCountry);
        return rows.stream().findFirst();
    }

    private VisaCheckItem evaluate(String countryCode, VisaCheckQuery query, VisaPolicyRow policyRow) {
        TransitRule transitRule = parseTransitRule(policyRow.transitConditions());
        int maxStayDays = policyRow.visaDurationDays() == null ? -1 : policyRow.visaDurationDays();
        String notes = policyRow.notes();

        if ("NO".equalsIgnoreCase(policyRow.visaRequired())) {
            // Visa-free entry is still bounded by the configured stay limit, so extended stays downgrade to
            // REQUIRED instead of silently reporting VISA_FREE.
            if (maxStayDays > 0 && query.stayDays() > maxStayDays) {
                return new VisaCheckItem(
                        countryCode,
                        countryCode,
                        "REQUIRED",
                        maxStayDays,
                        false,
                        transitRule.durationLimitHours(),
                        "visa-free stay exceeds allowed duration; " + notesForOverflow(maxStayDays, query.stayDays(), notes));
            }
            return new VisaCheckItem(
                    countryCode,
                    countryCode,
                    "VISA_FREE",
                    maxStayDays,
                    transitRule.transitWithoutVisa(),
                    transitRule.durationLimitHours(),
                    normalizeNotes(notes));
        }

        if (transitRule.transitWithoutVisa()) {
            int requestedHours = query.stayDays() * 24;
            Integer maxTransitHours = transitRule.durationLimitHours();
            // Transit-free treatment is intentionally modeled separately from visa-free entry because the user
            // may only qualify within a short stopover window.
            if (maxTransitHours == null || requestedHours <= maxTransitHours) {
                return new VisaCheckItem(
                        countryCode,
                        countryCode,
                        "TRANSIT_FREE",
                        maxStayDays,
                        true,
                        maxTransitHours,
                        firstNonBlank(
                                notes,
                                maxTransitHours == null
                                        ? "transit without visa is allowed"
                                        : "transit without visa is allowed within " + maxTransitHours + " hours"));
            }
            return new VisaCheckItem(
                    countryCode,
                    countryCode,
                    "REQUIRED",
                    maxStayDays,
                    true,
                    maxTransitHours,
                    "transit-free window is limited to " + maxTransitHours + " hours; planned stay exceeds that limit");
        }

        return new VisaCheckItem(
                countryCode,
                countryCode,
                "REQUIRED",
                maxStayDays,
                false,
                transitRule.durationLimitHours(),
                normalizeNotes(notes));
    }

    private VisaCheckItem missingData(String countryCode, String passportCountry) {
        return new VisaCheckItem(
                countryCode,
                countryCode,
                "DATA_NOT_FOUND",
                -1,
                false,
                null,
                "visa policy data is not available for " + countryCode + " under passport " + passportCountry);
    }

    private TransitRule parseTransitRule(String transitConditions) {
        if (transitConditions == null || transitConditions.isBlank()) {
            return new TransitRule(false, null);
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(transitConditions);
            boolean transitWithoutVisa = jsonNode.path("transit_without_visa").asBoolean(false);
            Integer durationLimitHours = jsonNode.has("duration_limit_hours") && !jsonNode.path("duration_limit_hours").isNull()
                    ? jsonNode.path("duration_limit_hours").asInt()
                    : null;
            return new TransitRule(transitWithoutVisa, durationLimitHours);
        } catch (Exception exception) {
            // A malformed transit JSON blob should not take the whole tool down. Falling back to permissive
            // transit metadata keeps the tool returning a business result instead of an infrastructure error.
            return new TransitRule(true, null);
        }
    }

    private String notesForOverflow(int maxStayDays, int requestedStayDays, String notes) {
        String overflowNote = "max visa-free stay is " + maxStayDays + " days but requested stay is " + requestedStayDays + " days";
        if (notes == null || notes.isBlank()) {
            return overflowNote;
        }
        return overflowNote + "; " + notes.trim();
    }

    private String normalizeNotes(String notes) {
        return notes == null ? "" : notes.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static final class VisaPolicyRowMapper implements RowMapper<VisaPolicyRow> {

        @Override
        public VisaPolicyRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VisaPolicyRow(
                    rs.getString("destination_country"),
                    rs.getString("passport_country"),
                    rs.getString("visa_required"),
                    getInteger(rs, "visa_duration_days"),
                    rs.getString("transit_conditions"),
                    rs.getString("notes"));
        }
    }

    private static Integer getInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private record VisaPolicyRow(
            String destinationCountry,
            String passportCountry,
            String visaRequired,
            Integer visaDurationDays,
            String transitConditions,
            String notes) {
    }

    private record TransitRule(boolean transitWithoutVisa, Integer durationLimitHours) {
    }
}
