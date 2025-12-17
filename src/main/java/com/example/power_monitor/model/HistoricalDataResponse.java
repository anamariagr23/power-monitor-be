package com.example.power_monitor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataResponse {
    private LocalDate date;
    private Integer count;
    private List<PowerConsumption> data;
}
