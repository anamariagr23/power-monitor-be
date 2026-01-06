package com.example.power_monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeAverages {
    private Double avgPower;          // average global_active_power (kW)
    private Double avgVoltage;        // average voltage (V)
    private Double avgCurrent;        // average global_intensity (A)
    private Double avgReactivePower;  // average global_reactive_power (kW)
    private Instant startTime;
    private Instant endTime;
    private Long dataPoints;
}
