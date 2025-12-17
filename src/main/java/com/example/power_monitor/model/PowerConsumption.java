package com.example.power_monitor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PowerConsumption {

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            timezone = "UTC")
    private Instant timestamp;

    private Double globalActivePower;
    private Double globalReactivePower;
    private Double voltage;
    private Double globalIntensity;
    private Double subMetering1;
    private Double subMetering2;
    private Double subMetering3;
    private Boolean isRealtime = false;

    // Constructor without isRealtime (for CSV parsing)
    public PowerConsumption(Instant timestamp, Double globalActivePower,
                            Double globalReactivePower, Double voltage,
                            Double globalIntensity, Double subMetering1,
                            Double subMetering2, Double subMetering3) {
        this.timestamp = timestamp;
        this.globalActivePower = globalActivePower;
        this.globalReactivePower = globalReactivePower;
        this.voltage = voltage;
        this.globalIntensity = globalIntensity;
        this.subMetering1 = subMetering1;
        this.subMetering2 = subMetering2;
        this.subMetering3 = subMetering3;
        this.isRealtime = false;
    }
}
