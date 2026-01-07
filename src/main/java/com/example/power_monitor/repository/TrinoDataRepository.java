package com.example.power_monitor.repository;

import com.example.power_monitor.model.AggregatedStatistics;
import com.example.power_monitor.model.AlertData;
import com.example.power_monitor.model.PowerConsumption;
import com.example.power_monitor.model.RealtimeAverages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TrinoDataRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PowerConsumption> powerMapper = (rs, rowNum) -> PowerConsumption.builder()
            .timestamp(rs.getTimestamp("timestamp").toInstant())
            .globalActivePower((Double) rs.getObject("global_active_power"))
            .globalReactivePower((Double) rs.getObject("global_reactive_power"))
            .voltage((Double) rs.getObject("voltage"))
            .globalIntensity((Double) rs.getObject("global_intensity"))
            .subMetering1((Double) rs.getObject("sub_metering_1"))
            .subMetering2((Double) rs.getObject("sub_metering_2"))
            .subMetering3((Double) rs.getObject("sub_metering_3"))
            .isRealtime(false)
            .build();

    public LocalDate getLatestDate() {
        // If your schema/catalog is already set in the JDBC URL (…/hive/energy),
        // you can refer to the table as events_training.
        String sql = "SELECT CAST(max(\"timestamp\") AS DATE) AS d FROM events_training";
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return null;
            Date d = rs.getDate("d");
            return d == null ? null : d.toLocalDate();
        });
    }

    public List<LocalDate> getAvailableDates() {
        String sql = """
            SELECT DISTINCT CAST("timestamp" AS DATE) AS d
            FROM events_training
            ORDER BY d
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getDate("d").toLocalDate());
    }

    public List<PowerConsumption> getDataForDate(LocalDate dateUtc) {
        Instant start = dateUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = dateUtc.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        String sql = """
            SELECT
              "timestamp" AS timestamp,
              global_active_power,
              global_reactive_power,
              voltage,
              global_intensity,
              sub_metering_1,
              sub_metering_2,
              sub_metering_3
            FROM events_training
            WHERE "timestamp" >= ? AND "timestamp" < ?
            ORDER BY "timestamp"
        """;

        return jdbcTemplate.query(
                sql,
                powerMapper,
                Timestamp.from(start),
                Timestamp.from(end)
        );
    }

    /**
     * Get historical data for a specific hour on a given date.
     * Returns 1 hour of data with 1-minute intervals (up to 60 records).
     *
     * @param dateTimeUtc The start of the hour (minute should be 00)
     * @return List of PowerConsumption records for that hour
     */
    public List<PowerConsumption> getDataForHour(java.time.LocalDateTime dateTimeUtc) {
        // Truncate to hour to ensure we start at :00
        java.time.LocalDateTime truncated = dateTimeUtc.truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        Instant start = truncated.toInstant(ZoneOffset.UTC);
        Instant end = truncated.plusHours(1).toInstant(ZoneOffset.UTC);

        log.info("Fetching historical data for hour: {} to {}", start, end);

        String sql = """
            SELECT
              "timestamp" AS timestamp,
              global_active_power,
              global_reactive_power,
              voltage,
              global_intensity,
              sub_metering_1,
              sub_metering_2,
              sub_metering_3
            FROM events_training
            WHERE "timestamp" >= ? AND "timestamp" < ?
                AND global_active_power IS NOT NULL
            ORDER BY "timestamp"
        """;

        return jdbcTemplate.query(
                sql,
                powerMapper,
                Timestamp.from(start),
                Timestamp.from(end)
        );
    }

    public List<PowerConsumption> getDataInRange(LocalDate startDateUtc, LocalDate endDateUtcInclusive) {
        Instant start = startDateUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = endDateUtcInclusive.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        String sql = """
            SELECT
              "timestamp" AS timestamp,
              global_active_power,
              global_reactive_power,
              voltage,
              global_intensity,
              sub_metering_1,
              sub_metering_2,
              sub_metering_3
            FROM events_training
            WHERE "timestamp" >= ? AND "timestamp" < ?
            ORDER BY "timestamp"
        """;

        return jdbcTemplate.query(
                sql,
                powerMapper,
                Timestamp.from(start),
                Timestamp.from(endExclusive)
        );
    }

    /**
     * Get aggregated statistics for a date range using SQL aggregations.
     * This is much more efficient than fetching all rows and computing in Java.
     */
    public AggregatedStatistics getAggregatedStatistics(LocalDate startDateUtc, LocalDate endDateUtcInclusive) {
        Instant start = startDateUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = endDateUtcInclusive.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        log.info("Fetching aggregated statistics from {} to {}", start, endExclusive);

        String sql = """
            SELECT
                COUNT(*) AS total_data_points,
                SUM(global_active_power) / 60.0 AS total_energy_kwh,
                AVG(global_active_power) AS avg_power,
                MIN(global_active_power) AS min_power,
                MAX(global_active_power) AS max_power,
                AVG(voltage) AS avg_voltage,
                MIN(voltage) AS min_voltage,
                MAX(voltage) AS max_voltage,
                AVG(global_intensity) AS avg_current,
                MIN(global_intensity) AS min_current,
                MAX(global_intensity) AS max_current,
                AVG(global_reactive_power) AS avg_reactive_power,
                MIN(global_reactive_power) AS min_reactive_power,
                MAX(global_reactive_power) AS max_reactive_power,
                SUM(sub_metering_1) / 1000.0 AS kitchen_kwh,
                SUM(sub_metering_2) / 1000.0 AS laundry_kwh,
                SUM(sub_metering_3) / 1000.0 AS hvac_kwh,
                SUM(GREATEST(0, (global_active_power * 1000 / 60) - sub_metering_1 - sub_metering_2 - sub_metering_3)) / 1000.0 AS other_kwh
            FROM events_training
            WHERE "timestamp" >= ? AND "timestamp" < ?
                AND global_active_power IS NOT NULL
        """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return null;
            return AggregatedStatistics.builder()
                    .totalDataPoints(rs.getLong("total_data_points"))
                    .totalEnergyKwh(rs.getDouble("total_energy_kwh"))
                    .avgPower(rs.getDouble("avg_power"))
                    .minPower(rs.getDouble("min_power"))
                    .maxPower(rs.getDouble("max_power"))
                    .avgVoltage(rs.getDouble("avg_voltage"))
                    .minVoltage(rs.getDouble("min_voltage"))
                    .maxVoltage(rs.getDouble("max_voltage"))
                    .avgCurrent(rs.getDouble("avg_current"))
                    .minCurrent(rs.getDouble("min_current"))
                    .maxCurrent(rs.getDouble("max_current"))
                    .avgReactivePower(rs.getDouble("avg_reactive_power"))
                    .minReactivePower(rs.getDouble("min_reactive_power"))
                    .maxReactivePower(rs.getDouble("max_reactive_power"))
                    .kitchenKwh(rs.getDouble("kitchen_kwh"))
                    .laundryKwh(rs.getDouble("laundry_kwh"))
                    .hvacKwh(rs.getDouble("hvac_kwh"))
                    .otherKwh(rs.getDouble("other_kwh"))
                    .build();
        }, Timestamp.from(start), Timestamp.from(endExclusive));
    }

    /**
     * Get peak usage (timestamp and power) for a date range.
     */
    public AggregatedStatistics getPeakUsage(LocalDate startDateUtc, LocalDate endDateUtcInclusive) {
        Instant start = startDateUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = endDateUtcInclusive.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        String sql = """
            SELECT "timestamp", global_active_power
            FROM events_training
            WHERE "timestamp" >= ? AND "timestamp" < ?
                AND global_active_power IS NOT NULL
            ORDER BY global_active_power DESC
            LIMIT 1
        """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return null;
            return AggregatedStatistics.builder()
                    .peakTimestamp(rs.getTimestamp("timestamp").toInstant())
                    .peakPower(rs.getDouble("global_active_power"))
                    .build();
        }, Timestamp.from(start), Timestamp.from(endExclusive));
    }

    /**
     * Get averages for real-time display (power, voltage, current, reactive power).
     */
    public RealtimeAverages getAveragesForTimeRange(Instant startTime, Instant endTime) {
        log.info("Fetching averages from {} to {}", startTime, endTime);

        String sql = """
            SELECT
                AVG(global_active_power) AS avg_power,
                AVG(voltage) AS avg_voltage,
                AVG(global_intensity) AS avg_current,
                AVG(global_reactive_power) AS avg_reactive_power,
                COUNT(*) AS data_points
            FROM events_training
            WHERE "timestamp" >= ? AND "timestamp" < ?
                AND global_active_power IS NOT NULL
        """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return null;
            return RealtimeAverages.builder()
                    .avgPower(rs.getDouble("avg_power"))
                    .avgVoltage(rs.getDouble("avg_voltage"))
                    .avgCurrent(rs.getDouble("avg_current"))
                    .avgReactivePower(rs.getDouble("avg_reactive_power"))
                    .dataPoints(rs.getLong("data_points"))
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
        }, Timestamp.from(startTime), Timestamp.from(endTime));
    }

    /**
     * Get alert threshold: average of daily max active power across all data.
     */
    public AlertData getAlertThreshold() {
        log.info("Calculating alert threshold (avg of daily max power)");

        String sql = """
            SELECT AVG(daily_max) AS avg_max_daily_power, COUNT(*) AS days_analyzed
            FROM (
                SELECT CAST("timestamp" AS DATE) AS day, MAX(global_active_power) AS daily_max
                FROM events_training
                WHERE global_active_power IS NOT NULL
                GROUP BY CAST("timestamp" AS DATE)
            ) daily_maxes
        """;

        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return null;
            double avgMax = rs.getDouble("avg_max_daily_power");
            return AlertData.builder()
                    .avgMaxDailyPower(avgMax)
                    .daysAnalyzed(rs.getLong("days_analyzed"))
                    .currentThreshold(avgMax * 1.2) // 20% above average as threshold
                    .build();
        });
    }
}

