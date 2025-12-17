package com.example.power_monitor.controller;

import com.example.power_monitor.model.HistoricalDataResponse;
import com.example.power_monitor.service.HistoricalDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/historical")
@RequiredArgsConstructor
public class HistoricalDataController {

    private final HistoricalDataService historicalDataService;

    @GetMapping("/{date}")
    public ResponseEntity<HistoricalDataResponse> getHistoricalData(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("REST request for historical data on date: {}", date);

        HistoricalDataResponse response = historicalDataService.getDataForDate(date);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dates")
    public ResponseEntity<List<LocalDate>> getAvailableDates() {
        log.info("REST request for available dates");

        List<LocalDate> dates = historicalDataService.getAvailableDates();

        return ResponseEntity.ok(dates);
    }
}
