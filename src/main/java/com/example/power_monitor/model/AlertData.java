package com.example.power_monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertData {
    private Double avgMaxDailyPower;  // AVG of (MAX power per day)
    private Long daysAnalyzed;
    private Double currentThreshold;  // Calculated threshold for alerts
}
