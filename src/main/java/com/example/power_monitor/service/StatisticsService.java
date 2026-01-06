package com.example.power_monitor.service;

import com.example.power_monitor.model.AggregatedStatistics;
import com.example.power_monitor.model.HouseStatistics;
import com.example.power_monitor.repository.TrinoDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final TrinoDataRepository trinoDataRepository;

    // Average electricity rate (USD per kWh) - adjust as needed
    private static final double ELECTRICITY_RATE = 0.12;

    public HouseStatistics getLastDayStatistics() {
        log.info("Calculating statistics for last day using SQL aggregations");

        List<LocalDate> availableDates = trinoDataRepository.getAvailableDates();
        if (availableDates.isEmpty()) {
            return createEmptyStats("last_day");
        }

        LocalDate lastDate = availableDates.get(availableDates.size() - 1);
        return getStatisticsForRange(lastDate, lastDate, "last_day");
    }

    public HouseStatistics getLastWeekStatistics() {
        log.info("Calculating statistics for last week using SQL aggregations");

        List<LocalDate> availableDates = trinoDataRepository.getAvailableDates();
        if (availableDates.isEmpty()) {
            return createEmptyStats("last_week");
        }

        LocalDate endDate = availableDates.get(availableDates.size() - 1);
        LocalDate startDate = endDate.minusDays(7);

        return getStatisticsForRange(startDate, endDate, "last_week");
    }

    public HouseStatistics getLastMonthStatistics() {
        log.info("Calculating statistics for last month using SQL aggregations");

        List<LocalDate> availableDates = trinoDataRepository.getAvailableDates();
        if (availableDates.isEmpty()) {
            return createEmptyStats("last_month");
        }

        LocalDate endDate = availableDates.get(availableDates.size() - 1);
        LocalDate startDate = endDate.minusDays(30);

        return getStatisticsForRange(startDate, endDate, "last_month");
    }

    public HouseStatistics getCustomRangeStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating statistics for custom range: {} to {} using SQL aggregations", startDate, endDate);
        return getStatisticsForRange(startDate, endDate, "custom");
    }

    /**
     * Get statistics for a date range using efficient SQL aggregations.
     */
    private HouseStatistics getStatisticsForRange(LocalDate startDate, LocalDate endDate, String period) {
        // Fetch aggregated stats from Trino (single query for all metrics)
        AggregatedStatistics stats = trinoDataRepository.getAggregatedStatistics(startDate, endDate);
        
        if (stats == null || stats.getTotalDataPoints() == 0) {
            log.warn("No data found for range {} to {}", startDate, endDate);
            return createEmptyStats(period);
        }

        // Fetch peak usage info (separate query to get timestamp)
        AggregatedStatistics peakInfo = trinoDataRepository.getPeakUsage(startDate, endDate);

        log.info("Building statistics from {} data points", stats.getTotalDataPoints());

        // Calculate room breakdown totals and percentages
        double totalRoomConsumption = stats.getKitchenKwh() + stats.getLaundryKwh() 
                + stats.getHvacKwh() + stats.getOtherKwh();

        // Build peak usage info
        HouseStatistics.PeakUsageInfo peakUsage = null;
        if (peakInfo != null && peakInfo.getPeakTimestamp() != null) {
            var peakTime = peakInfo.getPeakTimestamp().atZone(ZoneId.systemDefault()).toLocalDateTime();
            peakUsage = HouseStatistics.PeakUsageInfo.builder()
                    .timestamp(peakInfo.getPeakTimestamp().toString())
                    .power(peakInfo.getPeakPower())
                    .dayOfWeek(peakTime.getDayOfWeek().toString())
                    .hourOfDay(peakTime.getHour())
                    .build();
        }

        // Build and return the full statistics object
        return HouseStatistics.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalDataPoints(stats.getTotalDataPoints())
                .period(period)
                // Active Power
                .avgActivePower(stats.getAvgPower())
                .maxActivePower(stats.getMaxPower())
                .minActivePower(stats.getMinPower())
                .totalActivePowerKwh(stats.getTotalEnergyKwh())
                // Reactive Power
                .avgReactivePower(stats.getAvgReactivePower())
                .maxReactivePower(stats.getMaxReactivePower())
                .minReactivePower(stats.getMinReactivePower())
                // Voltage
                .avgVoltage(stats.getAvgVoltage())
                .maxVoltage(stats.getMaxVoltage())
                .minVoltage(stats.getMinVoltage())
                // Current/Intensity
                .avgIntensity(stats.getAvgCurrent())
                .maxIntensity(stats.getMaxCurrent())
                .minIntensity(stats.getMinCurrent())
                // Room breakdown
                .kitchen(createSubMeteringStats("Kitchen", stats.getKitchenKwh(), totalRoomConsumption))
                .laundry(createSubMeteringStats("Laundry", stats.getLaundryKwh(), totalRoomConsumption))
                .hvac(createSubMeteringStats("HVAC", stats.getHvacKwh(), totalRoomConsumption))
                .other(createSubMeteringStats("Other", stats.getOtherKwh(), totalRoomConsumption))
                // Cost estimation
                .estimatedCostUSD(stats.getTotalEnergyKwh() * ELECTRICITY_RATE)
                // Peak usage
                .peakUsage(peakUsage)
                .build();
    }

    private HouseStatistics.SubMeteringStats createSubMeteringStats(
            String name,
            double totalKwh,
            double totalConsumptionKwh) {

        double percentage = totalConsumptionKwh > 0 ? (totalKwh / totalConsumptionKwh) * 100 : 0;

        return HouseStatistics.SubMeteringStats.builder()
                .name(name)
                .totalWattHour(totalKwh * 1000) // Convert kWh back to Wh for compatibility
                .totalKwh(totalKwh)
                .percentage(percentage)
                .build();
    }

    private HouseStatistics createEmptyStats(String period) {
        return HouseStatistics.builder()
                .period(period)
                .totalDataPoints(0L)
                .avgActivePower(0.0)
                .maxActivePower(0.0)
                .minActivePower(0.0)
                .totalActivePowerKwh(0.0)
                .avgReactivePower(0.0)
                .maxReactivePower(0.0)
                .minReactivePower(0.0)
                .avgVoltage(0.0)
                .maxVoltage(0.0)
                .minVoltage(0.0)
                .avgIntensity(0.0)
                .maxIntensity(0.0)
                .minIntensity(0.0)
                .estimatedCostUSD(0.0)
                .kitchen(createSubMeteringStats("Kitchen", 0, 0))
                .laundry(createSubMeteringStats("Laundry", 0, 0))
                .hvac(createSubMeteringStats("HVAC", 0, 0))
                .other(createSubMeteringStats("Other", 0, 0))
                .build();
    }
}
