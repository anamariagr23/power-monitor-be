package com.example.power_monitor.controller;

import com.example.power_monitor.model.HistoricalDataResponse;
import com.example.power_monitor.service.HistoricalDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/historical")
@RequiredArgsConstructor
public class HistoricalDataController {

    private final HistoricalDataService historicalDataService;

    /**
     * Get historical data for an entire day.
     * Returns all minute-by-minute records for the specified date.
     */
    @GetMapping("/{date}")
    public ResponseEntity<HistoricalDataResponse> getHistoricalData(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("REST request for historical data on date: {}", date);

        HistoricalDataResponse response = historicalDataService.getDataForDate(date);

        return ResponseEntity.ok(response);
    }

    /**
     * Get historical data for a specific hour.
     * Returns 1 hour of data with 1-minute intervals (up to 60 records).
     * The minute in the datetime will be truncated to :00.
     *
     * @param dateTime ISO datetime (e.g., 2006-12-16T14:00:00)
     * @return HistoricalDataResponse with data for that hour
     */
    @GetMapping("/hourly")
    public ResponseEntity<HistoricalDataResponse> getHistoricalDataByHour(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {

        log.info("REST request for historical data at hour: {}", dateTime);

        HistoricalDataResponse response = historicalDataService.getDataForHour(dateTime);

        return ResponseEntity.ok(response);
    }

    /**
     * Get list of available dates that have historical data.
     */
    @GetMapping("/dates")
    public ResponseEntity<List<LocalDate>> getAvailableDates() {
        log.info("REST request for available dates");

        List<LocalDate> dates = historicalDataService.getAvailableDates();

        return ResponseEntity.ok(dates);
    }
}
