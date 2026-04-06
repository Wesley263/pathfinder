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
 * 主应用侧基于 JDBC 的图快照构建器。
 *
 * <p>该实现把机场与航线表转换为 Redis 友好的图快照读模型。
 * 构建与发布保持分离，确保主程序对数据所有权和快照物化职责边界清晰。</p>
 */
@Component
public class JdbcGraphSnapshotBuilder implements GraphSnapshotBuilder {

    /** 快照版本号时间戳格式（UTC）。 */
    private static final DateTimeFormatter VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    /** 机场基础数据查询语句。 */
    private static final String AIRPORT_SQL = """
            SELECT id, iata_code, icao_code, name_en, name_cn, city, city_cn, country_code,
                   latitude, longitude, timezone, type, source
            FROM t_airport
            WHERE deleted = 0
              AND iata_code IS NOT NULL
              AND iata_code <> ''
            """;

            /** 航线与航司聚合查询语句。 */
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

    /** 用于查询的 JDBC 模板。 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造 JDBC 图快照构建器。
     *
     * @param jdbcTemplate JDBC 查询模板
     */
    public JdbcGraphSnapshotBuilder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 从主程序数据库读取机场与航线并构建新快照。
     *
     * @param graphKey 图逻辑标识
     * @return 可直接发布的不可变图快照读模型
     */
    @Override
    public GraphSnapshot build(String graphKey) {
        Instant generatedAt = Instant.now();
        List<GraphSnapshotNode> nodes = jdbcTemplate.query(AIRPORT_SQL, new AirportSnapshotRowMapper());
        List<GraphSnapshotEdge> edges = jdbcTemplate.query(ROUTE_SQL, new RouteSnapshotRowMapper());

        // 快照版本在构建时生成：Redis 保存不可变版本，MCP 侧只应消费已发布版本。
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

    /** 机场行到快照节点的映射器。 */
    private static final class AirportSnapshotRowMapper implements RowMapper<GraphSnapshotNode> {

        /**
         * 把数据库机场行映射为 GraphSnapshotNode。
         *
         * @param rs 结果集
         * @param rowNum 行号
         * @return 快照节点
         * @throws SQLException SQL 读取异常
         */
        @Override
        public GraphSnapshotNode mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, Object> attributes = new LinkedHashMap<>();
            putIfPresent(attributes, "icaoCode", rs.getString("icao_code"));
            putIfPresent(attributes, "type", rs.getString("type"));
            putIfPresent(attributes, "source", rs.getString("source"));
            // 节点只保留路径搜索必要属性，避免 Redis 读模型被数据库实体形状绑死。
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

    /** 航线行到快照边的映射器。 */
    private static final class RouteSnapshotRowMapper implements RowMapper<GraphSnapshotEdge> {

        /**
         * 把数据库航线行映射为 GraphSnapshotEdge。
         *
         * @param rs 结果集
         * @param rowNum 行号
         * @return 快照边
         * @throws SQLException SQL 读取异常
         */
        @Override
        public GraphSnapshotEdge mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, Object> attributes = new LinkedHashMap<>();
            putIfPresent(attributes, "codeshare", rs.getString("codeshare"));
            putIfPresent(attributes, "equipment", rs.getString("equipment"));
            // 航线边被压平为搜索友好指标，保证 MCP 恢复与搜索不反查 bootstrap 数据库结构。
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

    /**
     * 当值非空白时写入属性字典。
     *
     * @param attributes 属性字典
     * @param key 属性名
     * @param value 属性值
     */
    private static void putIfPresent(Map<String, Object> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }

    /**
     * 返回首个非空白字符串。
     *
     * @param values 候选值
     * @return 首个非空白值
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /**
     * 读取 double 字段并处理 SQL NULL。
     *
     * @param resultSet 结果集
     * @param column 列名
     * @return 列值，NULL 时返回 0
     * @throws SQLException SQL 读取异常
     */
    private static double getDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? 0.0 : value;
    }

    /**
     * 读取 int 字段并处理 SQL NULL。
     *
     * @param resultSet 结果集
     * @param column 列名
     * @return 列值，NULL 时返回 0
     * @throws SQLException SQL 读取异常
     */
    private static int getInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? 0 : value;
    }
}

