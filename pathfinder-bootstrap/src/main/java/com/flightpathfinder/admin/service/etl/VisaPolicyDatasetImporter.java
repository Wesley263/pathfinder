package com.flightpathfinder.admin.service.etl;

import com.flightpathfinder.admin.config.AdminDataEtlProperties;
import com.flightpathfinder.admin.service.AdminDatasetReloadResult;
import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/**
 * ETL importer for visa policy data.
 *
 * <p>This importer belongs to admin because it loads external policy content into the operational datasource
 * and reports dataset-level ETL outcomes to operators.
 */
@Service
public class VisaPolicyDatasetImporter implements AdminDatasetImporter {

    private static final String UPSERT_SQL = """
            INSERT INTO t_visa_policy (
                destination_country,
                passport_country,
                visa_required,
                visa_duration_days,
                transit_conditions,
                notes,
                created_at,
                updated_at,
                deleted
            )
            VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            ON CONFLICT (destination_country, passport_country) WHERE deleted = 0
            DO UPDATE SET
                visa_required = EXCLUDED.visa_required,
                visa_duration_days = EXCLUDED.visa_duration_days,
                transit_conditions = EXCLUDED.transit_conditions,
                notes = EXCLUDED.notes,
                updated_at = CURRENT_TIMESTAMP,
                deleted = 0
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final AdminDataEtlProperties properties;

    public VisaPolicyDatasetImporter(JdbcTemplate jdbcTemplate,
                                     ResourceLoader resourceLoader,
                                     AdminDataEtlProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    /**
     * Returns the stable admin dataset id for visa policy ETL.
     *
     * @return {@code visa}
     */
    @Override
    public String datasetId() {
        return "visa";
    }

    /**
     * Executes visa policy ETL from the configured resource location.
     *
     * @return dataset-level reload result with processed/upserted/failed counts
     */
    @Override
    public AdminDatasetReloadResult reload() {
        Instant startedAt = Instant.now();
        String sourceLocation = properties.getVisa().getLocation();
        Resource resource = resourceLoader.getResource(sourceLocation);
        if (!resource.exists()) {
            return new AdminDatasetReloadResult(
                    datasetId(),
                    "MISSING",
                    "visa policy resource does not exist",
                    List.of(sourceLocation),
                    0L,
                    0L,
                    0L,
                    startedAt,
                    Instant.now(),
                    Map.of("missingLocation", sourceLocation));
        }

        long processed = 0L;
        long upserted = 0L;
        long failed = 0L;
        try (InputStream inputStream = resource.getInputStream()) {
            Object loaded = new Yaml().load(inputStream);
            List<?> rows = loaded instanceof List<?> list ? list : List.of();
            for (Object row : rows) {
                processed++;
                try {
                    if (!(row instanceof Map<?, ?> item)) {
                        failed++;
                        continue;
                    }
                    String destinationCountry = normalizeCountryCode(item.get("destination_country"));
                    String passportCountry = normalizeCountryCode(item.get("passport_country"));
                    if (destinationCountry.isBlank() || passportCountry.isBlank()) {
                        failed++;
                        continue;
                    }
                    jdbcTemplate.update(
                            UPSERT_SQL,
                            destinationCountry,
                            passportCountry,
                            normalizeText(item.get("visa_required")),
                            integerValue(item.get("visa_duration_days")),
                            normalizeJsonText(item.get("transit_conditions")),
                            trimToNull(normalizeText(item.get("notes"))));
                    upserted++;
                } catch (RuntimeException exception) {
                    failed++;
                }
            }
            // Row-level failures remain visible to operators so partial policy coverage is easier to diagnose.
            String status = failed > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";
            String reason = failed > 0
                    ? "visa policy ETL completed with row-level failures"
                    : "visa policy ETL completed";
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("resourceType", resourceDescription(resource));
            details.put("rowsRead", processed);
            details.put("rowsUpserted", upserted);
            details.put("rowsFailed", failed);
            return new AdminDatasetReloadResult(
                    datasetId(),
                    status,
                    reason,
                    List.of(sourceLocation),
                    processed,
                    upserted,
                    failed,
                    startedAt,
                    Instant.now(),
                    Map.copyOf(details));
        } catch (Exception exception) {
            return new AdminDatasetReloadResult(
                    datasetId(),
                    "FAILED",
                    "visa policy ETL failed: " + exception.getMessage(),
                    List.of(sourceLocation),
                    processed,
                    upserted,
                    Math.max(failed, 1L),
                    startedAt,
                    Instant.now(),
                    Map.of("resourceType", resourceDescription(resource)));
        }
    }

    private String resourceDescription(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (Exception exception) {
            return resource.getDescription();
        }
    }

    private String normalizeCountryCode(Object value) {
        String normalized = normalizeText(value).toUpperCase();
        return normalized.length() == 2 ? normalized : "";
    }

    private String normalizeText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalizeJsonText(Object value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? "{}" : normalized;
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception exception) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
