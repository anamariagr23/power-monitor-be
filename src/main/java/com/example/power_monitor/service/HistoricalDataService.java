package com.example.power_monitor.service;

import com.example.power_monitor.model.HistoricalDataResponse;
import com.example.power_monitor.model.PowerConsumption;
import com.example.power_monitor.repository.TrinoDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    public List<LocalDate> getAvailableDates() {
        log.info("Fetching available dates from Hive/Trino");
        return trinoDataRepository.getAvailableDates();
    }
}
