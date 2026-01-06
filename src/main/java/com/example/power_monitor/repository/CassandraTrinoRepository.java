package com.example.power_monitor.repository;

import com.example.power_monitor.model.PowerConsumption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * Repository for querying Cassandra through Trino.
 * Uses the Cassandra catalog configured in Trino to access the energy.events_by_hour table.
 * 
 * Note: This requires a Cassandra connector configured in Trino with catalog name 'cassandra'.
 * If Cassandra is not available, methods will return empty results gracefully.
 */
@Slf4j
@Repository
public class CassandraTrinoRepository {

    private final JdbcTemplate jdbcTemplate;

    public CassandraTrinoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PowerConsumption> powerMapper = (rs, rowNum) -> PowerConsumption.builder()
            .timestamp(rs.getTimestamp("timestamp").toInstant())
            .globalActivePower(getDoubleOrNull(rs, "global_active_power"))
            .globalReactivePower(getDoubleOrNull(rs, "global_reactive_power"))
            .voltage(getDoubleOrNull(rs, "voltage"))
            .globalIntensity(getDoubleOrNull(rs, "global_intensity"))
            .subMetering1(getDoubleOrNull(rs, "sub_metering_1"))
            .subMetering2(getDoubleOrNull(rs, "sub_metering_2"))
            .subMetering3(getDoubleOrNull(rs, "sub_metering_3"))
            .isRealtime(true)
            .build();

    private Double getDoubleOrNull(java.sql.ResultSet rs, String column) {
        try {
            double val = rs.getDouble(column);
            return rs.wasNull() ? null : val;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the latest real-time data from Cassandra events_by_hour table.
     * Returns data for the last N minutes (minute-by-minute readings).
     * 
     * @param limit Number of records to return (e.g., 60 for last hour)
     * @return List of PowerConsumption records, newest first
     */
    public List<PowerConsumption> getLatestRealtimeData(int limit) {
        log.info("Fetching latest {} realtime records from Cassandra", limit);

        // Query Cassandra through Trino
        // Note: Uses fully qualified table name with cassandra catalog
        String sql = """
            SELECT
                "timestamp",
                global_active_power,
                global_reactive_power,
                voltage,
                global_intensity,
                sub_metering_1,
                sub_metering_2,
                sub_metering_3
            FROM cassandra.energy.events_by_hour
            ORDER BY "timestamp" DESC
            LIMIT ?
        """;

        try {
            List<PowerConsumption> results = jdbcTemplate.query(sql, powerMapper, limit);
            log.info("Retrieved {} realtime records from Cassandra", results.size());
            return results;
        } catch (Exception e) {
            log.warn("Failed to query Cassandra through Trino: {}. Returning empty list.", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get real-time data for a specific time range from Cassandra.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of PowerConsumption records
     */
    public List<PowerConsumption> getRealtimeDataInRange(java.time.Instant startTime, java.time.Instant endTime) {
        log.info("Fetching realtime data from {} to {}", startTime, endTime);

        String sql = """
            SELECT
                "timestamp",
                global_active_power,
                global_reactive_power,
                voltage,
                global_intensity,
                sub_metering_1,
                sub_metering_2,
                sub_metering_3
            FROM cassandra.energy.events_by_hour
            WHERE "timestamp" >= ? AND "timestamp" < ?
            ORDER BY "timestamp"
        """;

        try {
            List<PowerConsumption> results = jdbcTemplate.query(
                    sql,
                    powerMapper,
                    java.sql.Timestamp.from(startTime),
                    java.sql.Timestamp.from(endTime)
            );
            log.info("Retrieved {} realtime records in range", results.size());
            return results;
        } catch (Exception e) {
            log.warn("Failed to query Cassandra through Trino: {}. Returning empty list.", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Check if Cassandra connection is available through Trino.
     */
    public boolean isAvailable() {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT 1 FROM cassandra.energy.events_by_hour LIMIT 1",
                    Integer.class
            );
            return true;
        } catch (Exception e) {
            log.debug("Cassandra not available: {}", e.getMessage());
            return false;
        }
    }
}
