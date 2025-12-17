package com.example.power_monitor.controller;

import com.example.power_monitor.model.HouseStatistics;
import com.example.power_monitor.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/last-day")
    public ResponseEntity<HouseStatistics> getLastDayStatistics() {
        log.info("REST request for last day statistics");
        HouseStatistics stats = statisticsService.getLastDayStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/last-week")
    public ResponseEntity<HouseStatistics> getLastWeekStatistics() {
        log.info("REST request for last week statistics");
        HouseStatistics stats = statisticsService.getLastWeekStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/last-month")
    public ResponseEntity<HouseStatistics> getLastMonthStatistics() {
        log.info("REST request for last month statistics");
        HouseStatistics stats = statisticsService.getLastMonthStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/custom")
    public ResponseEntity<HouseStatistics> getCustomRangeStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("REST request for custom range statistics: {} to {}", startDate, endDate);

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }

        HouseStatistics stats = statisticsService.getCustomRangeStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }
}
