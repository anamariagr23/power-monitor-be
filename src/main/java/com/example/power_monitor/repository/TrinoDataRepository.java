package com.example.power_monitor.repository;

import com.example.power_monitor.model.PowerConsumption;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

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
}
