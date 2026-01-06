package com.example.power_monitor.controller;

import com.example.power_monitor.model.AlertData;
import com.example.power_monitor.model.PowerConsumption;
import com.example.power_monitor.model.RealtimeAverages;
import com.example.power_monitor.service.RealtimeDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for real-time data endpoints.
 * 
 * Provides:
 * - Averages for card display (from Hive/HDFS through Trino)
 * - Latest real-time data (from Cassandra through Trino)
 * - Alert threshold data
 */
@Slf4j
@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealtimeApiController {

    private final RealtimeDataService realtimeDataService;

    /**
     * Get averages for a specific time range.
     * Used for displaying card metrics on the real-time page.
     * 
     * @param start Start of time range (ISO-8601 format)
     * @param end End of time range (ISO-8601 format)
     * @return RealtimeAverages containing avgPower, avgVoltage, avgCurrent, avgReactivePower
     */
    @GetMapping("/averages")
    public ResponseEntity<RealtimeAverages> getAverages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        log.info("REST request for realtime averages: {} to {}", start, end);
        
        Instant startInstant = start.toInstant(ZoneOffset.UTC);
        Instant endInstant = end.toInstant(ZoneOffset.UTC);
        
        RealtimeAverages averages = realtimeDataService.getAveragesForTimeRange(startInstant, endInstant);
        
        return ResponseEntity.ok(averages);
    }

    /**
     * Get averages for a specific hour (minute fixed to 00).
     * Convenience endpoint for hourly averages display.
     * 
     * @param dateTime The datetime (minute will be truncated to 00)
     * @return RealtimeAverages for the hour starting at the truncated datetime
     */
    @GetMapping("/averages/hourly")
    public ResponseEntity<RealtimeAverages> getHourlyAverages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        
        log.info("REST request for hourly averages at: {}", dateTime);
        
        RealtimeAverages averages = realtimeDataService.getAveragesForHour(dateTime);
        
        return ResponseEntity.ok(averages);
    }

    /**
     * Get the latest real-time data from Cassandra.
     * Returns minute-by-minute data for display range of 1 hour.
     * 
     * @param minutes Number of minutes of data to retrieve (default 60)
     * @return List of PowerConsumption records
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestData(
            @RequestParam(defaultValue = "60") int minutes) {
        
        log.info("REST request for latest {} minutes of realtime data", minutes);
        
        List<PowerConsumption> data = realtimeDataService.getLatestRealtimeData(minutes);
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", data.size());
        response.put("requestedMinutes", minutes);
        response.put("data", data);
        response.put("sourceAvailable", realtimeDataService.isRealtimeSourceAvailable());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get alert threshold data.
     * Calculates average of max daily active power across all historical data.
     * 
     * @return AlertData containing avgMaxDailyPower, daysAnalyzed, and currentThreshold
     */
    @GetMapping("/alerts")
    public ResponseEntity<AlertData> getAlerts() {
        log.info("REST request for alert threshold data");
        
        AlertData alertData = realtimeDataService.getAlertThreshold();
        
        return ResponseEntity.ok(alertData);
    }

    /**
     * Health check for real-time data sources.
     * 
     * @return Status of Cassandra connection
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("REST request for realtime health check");
        
        Map<String, Object> health = new HashMap<>();
        health.put("cassandraAvailable", realtimeDataService.isRealtimeSourceAvailable());
        health.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(health);
    }
}
