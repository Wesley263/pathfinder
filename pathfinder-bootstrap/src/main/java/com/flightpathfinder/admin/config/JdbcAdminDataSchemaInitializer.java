package com.flightpathfinder.admin.config;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 确保引导数据源中存在管理端 ETL 所需表结构。
 *
 * <p>该初始化器归属管理端功能，用于支撑管理触发的数据导入与诊断流程，而非直接服务用户请求链路。
 */
@Component
public class JdbcAdminDataSchemaInitializer {

    private static final List<String> DDL = List.of(
            """
            CREATE TABLE IF NOT EXISTS t_airport (
                id BIGSERIAL PRIMARY KEY,
                iata_code VARCHAR(10) UNIQUE,
                icao_code VARCHAR(10),
                name_en VARCHAR(255),
                name_cn VARCHAR(255),
                city VARCHAR(100),
                city_cn VARCHAR(100),
                country VARCHAR(100),
                country_code VARCHAR(2),
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION,
                altitude INT,
                timezone VARCHAR(50),
                type VARCHAR(50),
                source VARCHAR(50),
                created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                deleted SMALLINT DEFAULT 0
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_airport_iata
                ON t_airport (iata_code)
                WHERE deleted = 0
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_airport_country
                ON t_airport (country_code)
                WHERE deleted = 0
            """,
            """
            CREATE TABLE IF NOT EXISTS t_airline (
                id BIGSERIAL PRIMARY KEY,
                airline_id INT UNIQUE,
                name VARCHAR(255),
                alias VARCHAR(255),
                iata_code VARCHAR(5),
                icao_code VARCHAR(10),
                callsign VARCHAR(100),
                country VARCHAR(100),
                country_code VARCHAR(10),
                active VARCHAR(1) DEFAULT 'Y',
                airline_type VARCHAR(10),
                created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                deleted SMALLINT DEFAULT 0
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_airline_iata
                ON t_airline (iata_code)
                WHERE deleted = 0
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_airline_type
                ON t_airline (airline_type)
                WHERE deleted = 0
            """,
            """
            CREATE TABLE IF NOT EXISTS t_route (
                id BIGSERIAL PRIMARY KEY,
                airline_iata VARCHAR(5),
                airline_icao VARCHAR(10),
                source_airport_iata VARCHAR(10),
                source_airport_icao VARCHAR(10),
                dest_airport_iata VARCHAR(10),
                dest_airport_icao VARCHAR(10),
                codeshare VARCHAR(10),
                stops INT DEFAULT 0,
                equipment VARCHAR(50),
                distance_km DOUBLE PRECISION,
                base_price_cny DOUBLE PRECISION,
                duration_minutes INT,
                competition_count INT DEFAULT 1,
                created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                deleted SMALLINT DEFAULT 0
            )
            """,
            """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_route_unique
                ON t_route (airline_iata, source_airport_iata, dest_airport_iata)
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_route_pair
                ON t_route (source_airport_iata, dest_airport_iata)
                WHERE deleted = 0
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_route_airline
                ON t_route (airline_iata)
                WHERE deleted = 0
            """,
            """
            CREATE TABLE IF NOT EXISTS t_visa_policy (
                id BIGSERIAL PRIMARY KEY,
                destination_country VARCHAR(2) NOT NULL,
                passport_country VARCHAR(2) NOT NULL,
                visa_required VARCHAR(20),
                visa_duration_days INT,
                transit_conditions JSONB,
                notes TEXT,
                created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                deleted SMALLINT DEFAULT 0
            )
            """,
            """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_visa_policy_uk
                ON t_visa_policy (destination_country, passport_country)
                WHERE deleted = 0
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_visa_policy_dest
                ON t_visa_policy (destination_country)
                WHERE deleted = 0
            """,
            """
            CREATE TABLE IF NOT EXISTS t_city_cost (
                id BIGSERIAL PRIMARY KEY,
                iata_code VARCHAR(10) UNIQUE,
                city VARCHAR(100),
                country VARCHAR(100),
                country_code VARCHAR(2),
                daily_cost_usd DOUBLE PRECISION,
                accommodation_usd DOUBLE PRECISION,
                meal_cost_usd DOUBLE PRECISION,
                transportation_usd DOUBLE PRECISION,
                created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                deleted SMALLINT DEFAULT 0
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_city_cost_iata
                ON t_city_cost (iata_code)
                WHERE deleted = 0
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_city_cost_country
                ON t_city_cost (country_code)
                WHERE deleted = 0
            """
    );

    /**
        * 执行管理端 ETL 数据集所需的 DDL 语句集合。
     *
     * @param jdbcTemplate JDBC entry point used to create or update the admin-managed tables and indexes
     */
    public JdbcAdminDataSchemaInitializer(JdbcTemplate jdbcTemplate) {
        DDL.forEach(jdbcTemplate::execute);
    }
}
