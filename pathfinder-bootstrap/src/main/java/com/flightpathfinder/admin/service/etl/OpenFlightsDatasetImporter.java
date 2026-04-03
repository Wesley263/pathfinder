package com.flightpathfinder.admin.service.etl;

import com.flightpathfinder.admin.config.AdminDataEtlProperties;
import com.flightpathfinder.admin.service.AdminDatasetReloadResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * ETL importer for the OpenFlights-derived airport, airline and route dataset.
 *
 * <p>This importer lives on the admin surface because it is an operator-triggered ingestion pipeline, not a
 * user-facing runtime query service.
 */
@Service
public class OpenFlightsDatasetImporter implements AdminDatasetImporter {

    private static final CSVFormat OPENFLIGHTS_CSV = CSVFormat.DEFAULT.builder()
            .setQuote('"')
            .setNullString("\\N")
            .build();

    private static final String AIRPORT_UPSERT_SQL = """
            INSERT INTO t_airport (
                iata_code,
                icao_code,
                name_en,
                city,
                country,
                country_code,
                latitude,
                longitude,
                altitude,
                timezone,
                type,
                source,
                created_at,
                updated_at,
                deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            ON CONFLICT (iata_code)
            DO UPDATE SET
                icao_code = EXCLUDED.icao_code,
                name_en = EXCLUDED.name_en,
                city = EXCLUDED.city,
                country = EXCLUDED.country,
                country_code = EXCLUDED.country_code,
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                altitude = EXCLUDED.altitude,
                timezone = EXCLUDED.timezone,
                type = EXCLUDED.type,
                source = EXCLUDED.source,
                updated_at = CURRENT_TIMESTAMP,
                deleted = 0
            """;

    private static final String AIRLINE_UPSERT_SQL = """
            INSERT INTO t_airline (
                airline_id,
                name,
                alias,
                iata_code,
                icao_code,
                callsign,
                country,
                country_code,
                active,
                airline_type,
                created_at,
                updated_at,
                deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            ON CONFLICT (airline_id)
            DO UPDATE SET
                name = EXCLUDED.name,
                alias = EXCLUDED.alias,
                iata_code = EXCLUDED.iata_code,
                icao_code = EXCLUDED.icao_code,
                callsign = EXCLUDED.callsign,
                country = EXCLUDED.country,
                country_code = EXCLUDED.country_code,
                active = EXCLUDED.active,
                airline_type = EXCLUDED.airline_type,
                updated_at = CURRENT_TIMESTAMP,
                deleted = 0
            """;

    private static final String ROUTE_UPSERT_SQL = """
            INSERT INTO t_route (
                airline_iata,
                airline_icao,
                source_airport_iata,
                source_airport_icao,
                dest_airport_iata,
                dest_airport_icao,
                codeshare,
                stops,
                equipment,
                distance_km,
                base_price_cny,
                duration_minutes,
                competition_count,
                created_at,
                updated_at,
                deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            ON CONFLICT (airline_iata, source_airport_iata, dest_airport_iata)
            DO UPDATE SET
                airline_icao = EXCLUDED.airline_icao,
                source_airport_icao = EXCLUDED.source_airport_icao,
                dest_airport_icao = EXCLUDED.dest_airport_icao,
                codeshare = EXCLUDED.codeshare,
                stops = EXCLUDED.stops,
                equipment = EXCLUDED.equipment,
                distance_km = EXCLUDED.distance_km,
                base_price_cny = EXCLUDED.base_price_cny,
                duration_minutes = EXCLUDED.duration_minutes,
                competition_count = EXCLUDED.competition_count,
                updated_at = CURRENT_TIMESTAMP,
                deleted = 0
            """;

    private static final Set<String> LOW_COST_AIRLINES = Set.of(
            "SPRING AIRLINES", "AIRASIA", "RYANAIR", "EASYJET", "JETSTAR",
            "INDIGO", "CEBU PACIFIC", "SPIRIT", "SOUTHWEST", "PEACH",
            "SCOOT", "VIETJET", "LION AIR", "GOAIR", "WIZZ AIR",
            "FRONTIER", "ALLEGIANT", "SUN COUNTRY", "NORWEGIAN", "TUI");

    private static final Set<String> MID_MARKET_AIRLINES = Set.of(
            "HAINAN AIRLINES", "SICHUAN AIRLINES", "XIAMEN AIRLINES",
            "SILKAIR", "THAI SMILE", "ASIANA", "BEIJING CAPITAL",
            "SHANDONG AIRLINES", "SHENZHEN AIRLINES");

    private static final Map<String, String> COUNTRY_ALIASES = Map.ofEntries(
            Map.entry("CHINA", "CN"),
            Map.entry("HONG KONG", "HK"),
            Map.entry("MACAU", "MO"),
            Map.entry("TAIWAN", "TW"),
            Map.entry("SOUTH KOREA", "KR"),
            Map.entry("KOREA, REPUBLIC OF", "KR"),
            Map.entry("RUSSIA", "RU"),
            Map.entry("RUSSIAN FEDERATION", "RU"),
            Map.entry("UNITED STATES", "US"),
            Map.entry("UNITED STATES OF AMERICA", "US"),
            Map.entry("UNITED KINGDOM", "GB"),
            Map.entry("UK", "GB"),
            Map.entry("VIETNAM", "VN"),
            Map.entry("LAOS", "LA"),
            Map.entry("BOLIVIA", "BO"),
            Map.entry("VENEZUELA", "VE"),
            Map.entry("TANZANIA", "TZ"),
            Map.entry("IRAN", "IR"),
            Map.entry("MOLDOVA", "MD"),
            Map.entry("SYRIA", "SY"),
            Map.entry("BRUNEI", "BN"));

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final AdminDataEtlProperties properties;
    private final Map<String, String> countryCodeLookup;

    public OpenFlightsDatasetImporter(JdbcTemplate jdbcTemplate,
                                      ResourceLoader resourceLoader,
                                      AdminDataEtlProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
        this.countryCodeLookup = buildCountryCodeLookup();
    }

    /**
     * Returns the stable admin dataset id for OpenFlights ETL.
     *
     * @return {@code openflights}
     */
    @Override
    public String datasetId() {
        return "openflights";
    }

    /**
     * Executes OpenFlights ETL across airports, airlines and routes.
     *
     * @return dataset-level reload result with processed/upserted/failed counts and source details
     */
    @Override
    public AdminDatasetReloadResult reload() {
        Instant startedAt = Instant.now();
        String airportsLocation = properties.getOpenflights().getAirportsLocation();
        String airlinesLocation = properties.getOpenflights().getAirlinesLocation();
        String routesLocation = properties.getOpenflights().getRoutesLocation();
        List<String> sourceLocations = List.of(airportsLocation, airlinesLocation, routesLocation);

        Resource airportsResource = resourceLoader.getResource(airportsLocation);
        Resource airlinesResource = resourceLoader.getResource(airlinesLocation);
        Resource routesResource = resourceLoader.getResource(routesLocation);
        List<String> missingLocations = new ArrayList<>();
        if (!airportsResource.exists()) {
            missingLocations.add(airportsLocation);
        }
        if (!airlinesResource.exists()) {
            missingLocations.add(airlinesLocation);
        }
        if (!routesResource.exists()) {
            missingLocations.add(routesLocation);
        }
        if (!missingLocations.isEmpty()) {
            return new AdminDatasetReloadResult(
                    datasetId(),
                    "MISSING",
                    "openflights dataset is incomplete; one or more resources are missing",
                    sourceLocations,
                    0L,
                    0L,
                    0L,
                    startedAt,
                    Instant.now(),
                    Map.of("missingLocations", List.copyOf(missingLocations)));
        }

        try {
            // Airports and airlines are parsed first so route import can validate foreign references and compute
            // derived route attributes from the same source-of-truth rows.
            ParseOutcome<AirportRecord> airports = parseAirports(airportsResource);
            ParseOutcome<AirlineRecord> airlines = parseAirlines(airlinesResource);
            ParseOutcome<RouteRecord> routes = parseRoutes(routesResource, airports.records(), airlines.records());

            UpsertCounts airportUpserts = upsertAirports(airports.records());
            UpsertCounts airlineUpserts = upsertAirlines(airlines.records());
            UpsertCounts routeUpserts = upsertRoutes(routes.records());

            long processed = airports.processedCount() + airlines.processedCount() + routes.processedCount();
            long failed = airports.skippedCount() + airlines.skippedCount() + routes.skippedCount()
                    + airportUpserts.failedCount() + airlineUpserts.failedCount() + routeUpserts.failedCount();
            long upserted = airportUpserts.upsertedCount() + airlineUpserts.upsertedCount() + routeUpserts.upsertedCount();

            // Admin reload reports dataset-level detail instead of only one overall flag so operators can see
            // which slice of OpenFlights import degraded.
            String status = failed > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";
            String reason = failed > 0
                    ? "openflights ETL completed with row-level skips or failures"
                    : "openflights ETL completed";

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("airports", Map.of(
                    "source", resourceDescription(airportsResource),
                    "processedCount", airports.processedCount(),
                    "skippedCount", airports.skippedCount(),
                    "upsertedCount", airportUpserts.upsertedCount(),
                    "failedCount", airportUpserts.failedCount()));
            details.put("airlines", Map.of(
                    "source", resourceDescription(airlinesResource),
                    "processedCount", airlines.processedCount(),
                    "skippedCount", airlines.skippedCount(),
                    "upsertedCount", airlineUpserts.upsertedCount(),
                    "failedCount", airlineUpserts.failedCount()));
            details.put("routes", Map.of(
                    "source", resourceDescription(routesResource),
                    "processedCount", routes.processedCount(),
                    "skippedCount", routes.skippedCount(),
                    "upsertedCount", routeUpserts.upsertedCount(),
                    "failedCount", routeUpserts.failedCount()));

            return new AdminDatasetReloadResult(
                    datasetId(),
                    status,
                    reason,
                    sourceLocations,
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
                    "openflights ETL failed: " + exception.getMessage(),
                    sourceLocations,
                    0L,
                    0L,
                    1L,
                    startedAt,
                    Instant.now(),
                    Map.of());
        }
    }

    private ParseOutcome<AirportRecord> parseAirports(Resource resource) throws IOException {
        Map<String, AirportRecord> airports = new LinkedHashMap<>();
        long processed = 0L;
        long skipped = 0L;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = OPENFLIGHTS_CSV.parse(reader)) {
            for (CSVRecord record : parser) {
                processed++;
                String iataCode = normalizeCode(valueAt(record, 4));
                if (iataCode.isBlank()) {
                    skipped++;
                    continue;
                }
                String type = trimToEmpty(valueAt(record, 12));
                if (!"airport".equalsIgnoreCase(type)) {
                    skipped++;
                    continue;
                }
                double latitude = parseDouble(valueAt(record, 6));
                double longitude = parseDouble(valueAt(record, 7));
                if (latitude < -90D || latitude > 90D || longitude < -180D || longitude > 180D) {
                    skipped++;
                    continue;
                }
                if (airports.containsKey(iataCode)) {
                    skipped++;
                    continue;
                }
                airports.put(iataCode, new AirportRecord(
                        iataCode,
                        normalizeCode(valueAt(record, 5)),
                        trimToNull(valueAt(record, 1)),
                        trimToNull(valueAt(record, 2)),
                        trimToNull(valueAt(record, 3)),
                        countryCodeFor(valueAt(record, 3)),
                        latitude,
                        longitude,
                        parseInt(valueAt(record, 8)),
                        trimToNull(valueAt(record, 9)),
                        type,
                        trimToNull(valueAt(record, 13))));
            }
        }
        return new ParseOutcome<>(List.copyOf(airports.values()), processed, skipped);
    }

    private ParseOutcome<AirlineRecord> parseAirlines(Resource resource) throws IOException {
        Map<Integer, AirlineRecord> airlines = new LinkedHashMap<>();
        long processed = 0L;
        long skipped = 0L;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = OPENFLIGHTS_CSV.parse(reader)) {
            for (CSVRecord record : parser) {
                processed++;
                if (!"Y".equalsIgnoreCase(trimToEmpty(valueAt(record, 7)))) {
                    skipped++;
                    continue;
                }
                int airlineId = parseInt(valueAt(record, 0));
                String iataCode = normalizeCode(valueAt(record, 3));
                if (airlineId <= 0 || iataCode.isBlank() || "-".equals(iataCode)) {
                    skipped++;
                    continue;
                }
                if (airlines.containsKey(airlineId)) {
                    skipped++;
                    continue;
                }
                String name = trimToEmpty(valueAt(record, 1));
                airlines.put(airlineId, new AirlineRecord(
                        airlineId,
                        name,
                        trimToNull(valueAt(record, 2)),
                        iataCode,
                        normalizeCode(valueAt(record, 4)),
                        trimToNull(valueAt(record, 5)),
                        trimToNull(valueAt(record, 6)),
                        countryCodeFor(valueAt(record, 6)),
                        "Y",
                        determineAirlineType(name)));
            }
        }
        return new ParseOutcome<>(List.copyOf(airlines.values()), processed, skipped);
    }

    private ParseOutcome<RouteRecord> parseRoutes(Resource resource,
                                                  List<AirportRecord> airports,
                                                  List<AirlineRecord> airlines) throws IOException {
        Map<String, AirportRecord> airportIndex = new HashMap<>();
        airports.forEach(airport -> airportIndex.put(airport.iataCode(), airport));
        Map<String, AirlineRecord> airlineIndex = new HashMap<>();
        airlines.forEach(airline -> airlineIndex.put(airline.iataCode(), airline));

        Map<String, BasicRouteRecord> routes = new LinkedHashMap<>();
        long processed = 0L;
        long skipped = 0L;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = OPENFLIGHTS_CSV.parse(reader)) {
            for (CSVRecord record : parser) {
                processed++;
                String airlineIata = normalizeCode(valueAt(record, 0));
                String sourceIata = normalizeCode(valueAt(record, 2));
                String destIata = normalizeCode(valueAt(record, 4));
                if (airlineIata.isBlank() || sourceIata.isBlank() || destIata.isBlank() || sourceIata.equals(destIata)) {
                    skipped++;
                    continue;
                }
                AirportRecord source = airportIndex.get(sourceIata);
                AirportRecord destination = airportIndex.get(destIata);
                if (source == null || destination == null) {
                    skipped++;
                    continue;
                }
                String routeKey = airlineIata + "|" + sourceIata + "|" + destIata;
                if (routes.containsKey(routeKey)) {
                    skipped++;
                    continue;
                }
                double distanceKm = haversine(source.latitude(), source.longitude(), destination.latitude(), destination.longitude());
                if (distanceKm <= 0D) {
                    skipped++;
                    continue;
                }
                routes.put(routeKey, new BasicRouteRecord(
                        airlineIata,
                        trimToNull(valueAt(record, 6)),
                        parseInt(valueAt(record, 7)),
                        trimToNull(valueAt(record, 8)),
                        source,
                        destination,
                        distanceKm,
                        airlineIndex.get(airlineIata)));
            }
        }

        Map<String, Integer> pairCompetition = new HashMap<>();
        for (BasicRouteRecord route : routes.values()) {
            String pairKey = route.source().iataCode() + "|" + route.destination().iataCode();
            pairCompetition.merge(pairKey, 1, Integer::sum);
        }

        // Route import derives operational fields such as distance, duration and base price so downstream graph
        // and MCP flows can work without depending on 1.0 loader implementations.
        List<RouteRecord> finalRoutes = new ArrayList<>(routes.size());
        for (BasicRouteRecord route : routes.values()) {
            String pairKey = route.source().iataCode() + "|" + route.destination().iataCode();
            int competitionCount = Math.max(1, pairCompetition.getOrDefault(pairKey, 1));
            String airlineType = route.airline() == null ? "FSC" : route.airline().airlineType();
            finalRoutes.add(new RouteRecord(
                    route.airlineIata(),
                    route.airline() == null ? null : route.airline().icaoCode(),
                    route.source().iataCode(),
                    route.source().icaoCode(),
                    route.destination().iataCode(),
                    route.destination().icaoCode(),
                    route.codeshare(),
                    Math.max(0, route.stops()),
                    route.equipment(),
                    round(route.distanceKm()),
                    calculateBasePrice(route.distanceKm(), airlineType, competitionCount),
                    estimateDurationMinutes(route.distanceKm()),
                    competitionCount));
        }
        return new ParseOutcome<>(List.copyOf(finalRoutes), processed, skipped);
    }

    private UpsertCounts upsertAirports(List<AirportRecord> airports) {
        long upserted = 0L;
        long failed = 0L;
        for (AirportRecord airport : airports) {
            try {
                jdbcTemplate.update(
                        AIRPORT_UPSERT_SQL,
                        airport.iataCode(),
                        airport.icaoCode(),
                        airport.nameEn(),
                        airport.city(),
                        airport.country(),
                        airport.countryCode(),
                        airport.latitude(),
                        airport.longitude(),
                        airport.altitude(),
                        airport.timezone(),
                        airport.type(),
                        airport.source());
                upserted++;
            } catch (RuntimeException exception) {
                failed++;
            }
        }
        return new UpsertCounts(upserted, failed);
    }

    private UpsertCounts upsertAirlines(List<AirlineRecord> airlines) {
        long upserted = 0L;
        long failed = 0L;
        for (AirlineRecord airline : airlines) {
            try {
                jdbcTemplate.update(
                        AIRLINE_UPSERT_SQL,
                        airline.airlineId(),
                        airline.name(),
                        airline.alias(),
                        airline.iataCode(),
                        airline.icaoCode(),
                        airline.callsign(),
                        airline.country(),
                        airline.countryCode(),
                        airline.active(),
                        airline.airlineType());
                upserted++;
            } catch (RuntimeException exception) {
                failed++;
            }
        }
        return new UpsertCounts(upserted, failed);
    }

    private UpsertCounts upsertRoutes(List<RouteRecord> routes) {
        long upserted = 0L;
        long failed = 0L;
        for (RouteRecord route : routes) {
            try {
                jdbcTemplate.update(
                        ROUTE_UPSERT_SQL,
                        route.airlineIata(),
                        route.airlineIcao(),
                        route.sourceAirportIata(),
                        route.sourceAirportIcao(),
                        route.destAirportIata(),
                        route.destAirportIcao(),
                        route.codeshare(),
                        route.stops(),
                        route.equipment(),
                        route.distanceKm(),
                        route.basePriceCny(),
                        route.durationMinutes(),
                        route.competitionCount());
                upserted++;
            } catch (RuntimeException exception) {
                failed++;
            }
        }
        return new UpsertCounts(upserted, failed);
    }

    private String resourceDescription(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (Exception exception) {
            return resource.getDescription();
        }
    }

    private Map<String, String> buildCountryCodeLookup() {
        Map<String, String> lookup = new HashMap<>(COUNTRY_ALIASES);
        for (String isoCountry : Locale.getISOCountries()) {
            Locale locale = new Locale("", isoCountry);
            lookup.put(locale.getDisplayCountry(Locale.ENGLISH).toUpperCase(Locale.ROOT), isoCountry);
        }
        return Map.copyOf(lookup);
    }

    private String countryCodeFor(String countryName) {
        String normalized = trimToEmpty(countryName).toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        return countryCodeLookup.getOrDefault(normalized, normalized.length() == 2 ? normalized : null);
    }

    private String determineAirlineType(String airlineName) {
        String normalized = airlineName == null ? "" : airlineName.toUpperCase(Locale.ROOT);
        if (LOW_COST_AIRLINES.stream().anyMatch(normalized::contains)) {
            return "LCC";
        }
        if (MID_MARKET_AIRLINES.stream().anyMatch(normalized::contains)) {
            return "MID";
        }
        return "FSC";
    }

    private int estimateDurationMinutes(double distanceKm) {
        return Math.max(60, (int) Math.round(distanceKm / 850.0D * 60.0D + 40.0D));
    }

    private double calculateBasePrice(double distanceKm, String airlineType, int competitionCount) {
        double safeDistance = Math.max(distanceKm, 300D);
        double base = 180D + Math.pow(safeDistance, 0.78D) * 1.65D;
        double airlineFactor = switch (airlineType) {
            case "LCC" -> 0.86D;
            case "MID" -> 0.97D;
            default -> 1.08D;
        };
        double competitionFactor = Math.max(0.72D, 1.0D - (Math.max(competitionCount, 1) - 1) * 0.05D);
        double distanceSurcharge = safeDistance >= 7000D ? 320D : safeDistance >= 2500D ? 180D : 60D;
        return round(Math.max(150D, (base + distanceSurcharge) * airlineFactor * competitionFactor));
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088D;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private String valueAt(CSVRecord record, int index) {
        try {
            String value = record.get(index);
            return value == null || "\\N".equals(value) ? "" : value;
        } catch (Exception exception) {
            return "";
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String normalized = trimToEmpty(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCode(String value) {
        return trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(trimToEmpty(value));
        } catch (Exception exception) {
            return 0.0D;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(trimToEmpty(value));
        } catch (Exception exception) {
            return 0;
        }
    }

    private record ParseOutcome<T>(List<T> records, long processedCount, long skippedCount) {
    }

    private record UpsertCounts(long upsertedCount, long failedCount) {
    }

    private record AirportRecord(
            String iataCode,
            String icaoCode,
            String nameEn,
            String city,
            String country,
            String countryCode,
            double latitude,
            double longitude,
            int altitude,
            String timezone,
            String type,
            String source) {
    }

    private record AirlineRecord(
            int airlineId,
            String name,
            String alias,
            String iataCode,
            String icaoCode,
            String callsign,
            String country,
            String countryCode,
            String active,
            String airlineType) {
    }

    private record BasicRouteRecord(
            String airlineIata,
            String codeshare,
            int stops,
            String equipment,
            AirportRecord source,
            AirportRecord destination,
            double distanceKm,
            AirlineRecord airline) {
    }

    private record RouteRecord(
            String airlineIata,
            String airlineIcao,
            String sourceAirportIata,
            String sourceAirportIcao,
            String destAirportIata,
            String destAirportIcao,
            String codeshare,
            int stops,
            String equipment,
            double distanceKm,
            double basePriceCny,
            int durationMinutes,
            int competitionCount) {
    }
}
