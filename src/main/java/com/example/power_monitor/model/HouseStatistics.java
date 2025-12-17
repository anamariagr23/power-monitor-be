package com.example.power_monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseStatistics {

    // Time period info
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalDataPoints;
    private String period; // "last_day", "last_week", "last_month", "custom"

    // Global Active Power (kW)
    private Double avgActivePower;
    private Double maxActivePower;
    private Double minActivePower;
    private Double totalActivePowerKwh; // Total energy consumed

    // Global Reactive Power (kW)
    private Double avgReactivePower;
    private Double maxReactivePower;
    private Double minReactivePower;

    // Voltage (V)
    private Double avgVoltage;
    private Double maxVoltage;
    private Double minVoltage;

    // Current Intensity (A)
    private Double avgIntensity;
    private Double maxIntensity;
    private Double minIntensity;

    // Sub-metering breakdown
    private SubMeteringStats kitchen;     // Sub-metering 1
    private SubMeteringStats laundry;     // Sub-metering 2
    private SubMeteringStats hvac;        // Sub-metering 3
    private SubMeteringStats other;       // Calculated: Not measured by sub-meters

    // Cost estimation (optional)
    private Double estimatedCostUSD;      // Based on average electricity rate

    // Peak usage info
    private PeakUsageInfo peakUsage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubMeteringStats {
        private String name;
        private Double totalWattHour;
        private Double totalKwh;
        private Double percentage;       // Percentage of total consumption
        private Double avgPower;
        private Double maxPower;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakUsageInfo {
        private String timestamp;
        private Double power;
        private String dayOfWeek;
        private Integer hourOfDay;
    }
}
