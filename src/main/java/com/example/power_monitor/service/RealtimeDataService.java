package com.example.power_monitor.service;

import com.example.power_monitor.model.AlertData;
import com.example.power_monitor.model.PowerConsumption;
import com.example.power_monitor.model.RealtimeAverages;
import com.example.power_monitor.repository.CassandraTrinoRepository;
import com.example.power_monitor.repository.TrinoDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for real-time data operations.
 * Provides averages from Hive/HDFS and latest data from Cassandra.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeDataService {

    private final TrinoDataRepository trinoDataRepository;
    private final CassandraTrinoRepository cassandraTrinoRepository;

    /**
     * Get averages for the specified time range (for card display).
     * Data is fetched from Hive/HDFS through Trino.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return RealtimeAverages containing avg power, voltage, current, reactive power
     */
    public RealtimeAverages getAveragesForTimeRange(Instant startTime, Instant endTime) {
        log.info("Getting averages for time range: {} to {}", startTime, endTime);
        
        RealtimeAverages averages = trinoDataRepository.getAveragesForTimeRange(startTime, endTime);
        
        if (averages == null) {
            log.warn("No data found for time range, returning empty averages");
            return RealtimeAverages.builder()
                    .avgPower(0.0)
                    .avgVoltage(0.0)
                    .avgCurrent(0.0)
                    .avgReactivePower(0.0)
                    .dataPoints(0L)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
        }
        
        return averages;
    }

    /**
     * Get averages for a specific datetime with minute fixed to 00 (top of the hour).
     * Display range is 1 hour, interval is 1 minute.
     * 
     * @param dateTime The datetime (minute will be truncated to 00)
     * @return RealtimeAverages for the hour starting at the truncated datetime
     */
    public RealtimeAverages getAveragesForHour(LocalDateTime dateTime) {
        // Truncate to hour (minute fixed to 00)
        LocalDateTime truncated = dateTime.truncatedTo(ChronoUnit.HOURS);
        Instant startTime = truncated.toInstant(ZoneOffset.UTC);
        Instant endTime = truncated.plusHours(1).toInstant(ZoneOffset.UTC);
        
        log.info("Getting hourly averages for: {} (truncated from {})", truncated, dateTime);
        
        return getAveragesForTimeRange(startTime, endTime);
    }

    /**
     * Get the latest real-time data from Cassandra.
     * Returns whatever Cassandra has as the latest data (minute-by-minute).
     * 
     * @param minutes Number of minutes of data to retrieve (default 60 for 1 hour)
     * @return List of PowerConsumption records, newest first
     */
    public List<PowerConsumption> getLatestRealtimeData(int minutes) {
        log.info("Getting latest {} minutes of realtime data from Cassandra", minutes);
        
        List<PowerConsumption> data = cassandraTrinoRepository.getLatestRealtimeData(minutes);
        
        if (data.isEmpty()) {
            log.warn("No realtime data available from Cassandra");
        } else {
            log.info("Retrieved {} realtime records", data.size());
        }
        
        return data;
    }

    /**
     * Get alert threshold data.
     * Calculates the average of max daily active power across all data.
     * Formula: SELECT AVG(daily_max) FROM (SELECT MAX(power) GROUP BY day)
     * 
     * @return AlertData containing avgMaxDailyPower and threshold
     */
    public AlertData getAlertThreshold() {
        log.info("Calculating alert threshold");
        
        AlertData alertData = trinoDataRepository.getAlertThreshold();
        
        if (alertData == null) {
            log.warn("Could not calculate alert threshold, returning defaults");
            return AlertData.builder()
                    .avgMaxDailyPower(0.0)
                    .daysAnalyzed(0L)
                    .currentThreshold(0.0)
                    .build();
        }
        
        log.info("Alert threshold calculated: avgMaxDaily={}, threshold={}, daysAnalyzed={}",
                alertData.getAvgMaxDailyPower(),
                alertData.getCurrentThreshold(),
                alertData.getDaysAnalyzed());
        
        return alertData;
    }

    /**
     * Check if Cassandra real-time data source is available.
     */
    public boolean isRealtimeSourceAvailable() {
        return cassandraTrinoRepository.isAvailable();
    }
}
