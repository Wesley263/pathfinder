package com.flightpathfinder.flight.core.graph.snapshot;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotEdge;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotNode;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotVersions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * JDBC-backed snapshot builder on the bootstrap side.
 *
 * <p>This class turns normalized airport and route tables into a Redis-friendly graph snapshot
 * read model. It stays separate from publishing so the main program can keep data ownership
 * and snapshot materialization concerns distinct.</p>
 */
@Component
public class JdbcGraphSnapshotBuilder implements GraphSnapshotBuilder {

    private static final DateTimeFormatter VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private static final String AIRPORT_SQL = """
            SELECT id, iata_code, icao_code, name_en, name_cn, city, city_cn, country_code,
                   latitude, longitude, timezone, type, source
            FROM t_airport
            WHERE deleted = 0
              AND iata_code IS NOT NULL
              AND iata_code <> ''
            """;

    private static final String ROUTE_SQL = """
            SELECT r.id, r.airline_iata, r.source_airport_iata, r.dest_airport_iata, r.codeshare,
                   r.stops, r.equipment, r.distance_km, r.base_price_cny, r.duration_minutes, r.competition_count,
                   a.name AS airline_name, a.airline_type
            FROM t_route r
            LEFT JOIN t_airline a
              ON a.iata_code = r.airline_iata
             AND a.deleted = 0
            WHERE r.deleted = 0
              AND r.source_airport_iata IS NOT NULL
              AND r.source_airport_iata <> ''
              AND r.dest_airport_iata IS NOT NULL
              AND r.dest_airport_iata <> ''
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcGraphSnapshotBuilder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Builds a fresh snapshot by reading airports and routes from the main program database.
     *
     * @param graphKey logical graph identifier
     * @return immutable snapshot read model ready for publication
     */
    @Override
    public GraphSnapshot build(String graphKey) {
        Instant generatedAt = Instant.now();
        List<GraphSnapshotNode> nodes = jdbcTemplate.query(AIRPORT_SQL, new AirportSnapshotRowMapper());
        List<GraphSnapshotEdge> edges = jdbcTemplate.query(ROUTE_SQL, new RouteSnapshotRowMapper());

        // The snapshot version is generated at build time because Redis stores immutable
        // read-model versions and the MCP side should only ever consume a published snapshot.
        String snapshotVersion = VERSION_FORMATTER.format(generatedAt) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String sourceFingerprint = "airports=" + nodes.size() + ";edges=" + edges.size();

        return new GraphSnapshot(
                GraphSnapshotVersions.CURRENT_SCHEMA_VERSION,
                snapshotVersion,
                generatedAt,
                graphKey,
                sourceFingerprint,
                nodes,
                edges,
                Map.of(
                        "builder", "jdbc",
                        "airportCount", nodes.size(),
                        "edgeCount", edges.size()
                ));
    }

    private static final class AirportSnapshotRowMapper implements RowMapper<GraphSnapshotNode> {

        @Override
        public GraphSnapshotNode mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, Object> attributes = new LinkedHashMap<>();
            putIfPresent(attributes, "icaoCode", rs.getString("icao_code"));
            putIfPresent(attributes, "type", rs.getString("type"));
            putIfPresent(attributes, "source", rs.getString("source"));
            // Snapshot nodes deliberately keep only path-search-relevant attributes so the
            // Redis payload stays stable and the MCP side does not inherit DB entity shape.
            return new GraphSnapshotNode(
                    rs.getString("iata_code"),
                    rs.getString("iata_code"),
                    firstNonBlank(rs.getString("name_cn"), rs.getString("name_en"), rs.getString("iata_code")),
                    firstNonBlank(rs.getString("city_cn"), rs.getString("city"), ""),
                    rs.getString("country_code"),
                    getDouble(rs, "latitude"),
                    getDouble(rs, "longitude"),
                    firstNonBlank(rs.getString("timezone"), "UTC"),
                    false,
                    false,
                    0.0,
                    60,
                    Map.copyOf(attributes));
        }
    }

    private static final class RouteSnapshotRowMapper implements RowMapper<GraphSnapshotEdge> {

        @Override
        public GraphSnapshotEdge mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, Object> attributes = new LinkedHashMap<>();
            putIfPresent(attributes, "codeshare", rs.getString("codeshare"));
            putIfPresent(attributes, "equipment", rs.getString("equipment"));
            // Route edges are flattened into search-friendly metrics so the MCP server can
            // restore and search the graph without reaching back to the bootstrap DB schema.
            return new GraphSnapshotEdge(
                    "route:" + rs.getLong("id"),
                    rs.getString("source_airport_iata"),
                    rs.getString("dest_airport_iata"),
                    firstNonBlank(rs.getString("airline_iata"), "UNKNOWN"),
                    firstNonBlank(rs.getString("airline_name"), rs.getString("airline_iata"), "UNKNOWN"),
                    firstNonBlank(rs.getString("airline_type"), "FSC"),
                    getDouble(rs, "base_price_cny"),
                    getInt(rs, "duration_minutes"),
                    getDouble(rs, "distance_km"),
                    0.85,
                    true,
                    Math.max(1, getInt(rs, "competition_count")),
                    getInt(rs, "stops"),
                    Map.copyOf(attributes));
        }
    }

    private static void putIfPresent(Map<String, Object> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
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
        return resultSet.wasNull() ? 0.0 : value;
    }

    private static int getInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? 0 : value;
    }
}
