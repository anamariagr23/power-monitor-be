package com.example.power_monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Internal DTO for holding raw aggregated statistics from Trino SQL query.
 * Used to build the full HouseStatistics response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedStatistics {
    // Data points count
    private Long totalDataPoints;
    
    // Energy totals
    private Double totalEnergyKwh;
    
    // Active Power (kW)
    private Double avgPower;
    private Double minPower;
    private Double maxPower;
    
    // Voltage (V)
    private Double avgVoltage;
    private Double minVoltage;
    private Double maxVoltage;
    
    // Current Intensity (A)
    private Double avgCurrent;
    private Double minCurrent;
    private Double maxCurrent;
    
    // Reactive Power (kW)
    private Double avgReactivePower;
    private Double minReactivePower;
    private Double maxReactivePower;
    
    // Room breakdown (kWh)
    private Double kitchenKwh;
    private Double laundryKwh;
    private Double hvacKwh;
    private Double otherKwh;
    
    // Peak usage info
    private Instant peakTimestamp;
    private Double peakPower;
}
