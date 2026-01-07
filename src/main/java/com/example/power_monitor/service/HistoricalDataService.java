package com.example.power_monitor.service;

import com.example.power_monitor.model.HistoricalDataResponse;
import com.example.power_monitor.model.PowerConsumption;
import com.example.power_monitor.repository.TrinoDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataService {

    private final TrinoDataRepository trinoDataRepository;

    public HistoricalDataResponse getDataForDate(LocalDate date) {
        log.info("Fetching historical data for date: {}", date);

        List<PowerConsumption> data = trinoDataRepository.getDataForDate(date);

        log.info("Found {} records for date: {}", data.size(), date);

        return new HistoricalDataResponse(
                date,
                data.size(),
                data
        );
    }

    /**
     * Get historical data for a specific hour.
     * Returns 1 hour of data with 1-minute intervals.
     *
     * @param dateTime The datetime (minute will be truncated to :00)
     * @return HistoricalDataResponse containing up to 60 records
     */
    public HistoricalDataResponse getDataForHour(LocalDateTime dateTime) {
        // Truncate to hour
        LocalDateTime truncated = dateTime.truncatedTo(ChronoUnit.HOURS);
        log.info("Fetching historical data for hour: {} (truncated from {})", truncated, dateTime);

        List<PowerConsumption> data = trinoDataRepository.getDataForHour(truncated);

        log.info("Found {} records for hour: {}", data.size(), truncated);

        return new HistoricalDataResponse(
                truncated.toLocalDate(),
                data.size(),
                data
        );
    }

    public List<LocalDate> getAvailableDates() {
        log.info("Fetching available dates from Hive/Trino");
        return trinoDataRepository.getAvailableDates();
    }
}
