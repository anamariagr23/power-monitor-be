package com.example.power_monitor.service;


import com.example.power_monitor.model.HouseStatistics;
import com.example.power_monitor.model.PowerConsumption;
import com.example.power_monitor.repository.CsvDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CsvDataRepository csvDataRepository;

    // Average electricity rate (USD per kWh) - adjust as needed
    private static final double ELECTRICITY_RATE = 0.12;

    public HouseStatistics getLastDayStatistics() {
        log.info("Calculating statistics for last day");

        // Get most recent date from CSV
        List<LocalDate> availableDates = csvDataRepository.getAvailableDates();
        if (availableDates.isEmpty()) {
            return createEmptyStats("last_day");
        }

        LocalDate lastDate = availableDates.get(availableDates.size() - 1);
        List<PowerConsumption> data = csvDataRepository.getDataForDate(lastDate);

        return calculateStatistics(data, lastDate, lastDate, "last_day");
    }

    public HouseStatistics getLastWeekStatistics() {
        log.info("Calculating statistics for last week");

        List<LocalDate> availableDates = csvDataRepository.getAvailableDates();
        if (availableDates.isEmpty()) {
            return createEmptyStats("last_week");
        }

        LocalDate endDate = availableDates.get(availableDates.size() - 1);
        LocalDate startDate = endDate.minusDays(7);

        List<PowerConsumption> data = getDataInRange(startDate, endDate);
        return calculateStatistics(data, startDate, endDate, "last_week");
    }

    public HouseStatistics getLastMonthStatistics() {
        log.info("Calculating statistics for last month");

        List<LocalDate> availableDates = csvDataRepository.getAvailableDates();
        if (availableDates.isEmpty()) {
            return createEmptyStats("last_month");
        }

        LocalDate endDate = availableDates.get(availableDates.size() - 1);
        LocalDate startDate = endDate.minusDays(30);

        List<PowerConsumption> data = getDataInRange(startDate, endDate);
        return calculateStatistics(data, startDate, endDate, "last_month");
    }

    public HouseStatistics getCustomRangeStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating statistics for custom range: {} to {}", startDate, endDate);

        List<PowerConsumption> data = getDataInRange(startDate, endDate);
        return calculateStatistics(data, startDate, endDate, "custom");
    }

    private List<PowerConsumption> getDataInRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> availableDates = csvDataRepository.getAvailableDates();

        return availableDates.stream()
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                .flatMap(date -> csvDataRepository.getDataForDate(date).stream())
                .collect(Collectors.toList());
    }

    private HouseStatistics calculateStatistics(
            List<PowerConsumption> data,
            LocalDate startDate,
            LocalDate endDate,
            String period) {

        if (data.isEmpty()) {
            return createEmptyStats(period);
        }

        log.info("Calculating stats for {} data points", data.size());

        // Active Power statistics
        double avgActivePower = data.stream()
                .mapToDouble(PowerConsumption::getGlobalActivePower)
                .average()
                .orElse(0.0);

        double maxActivePower = data.stream()
                .mapToDouble(PowerConsumption::getGlobalActivePower)
                .max()
                .orElse(0.0);

        double minActivePower = data.stream()
                .mapToDouble(PowerConsumption::getGlobalActivePower)
                .min()
                .orElse(0.0);

        // Total energy (kW * minutes / 60 = kWh)
        double totalActivePowerKwh = data.stream()
                .mapToDouble(PowerConsumption::getGlobalActivePower)
                .sum() / 60.0;

        // Reactive Power statistics
        double avgReactivePower = data.stream()
                .mapToDouble(PowerConsumption::getGlobalReactivePower)
                .average()
                .orElse(0.0);

        double maxReactivePower = data.stream()
                .mapToDouble(PowerConsumption::getGlobalReactivePower)
                .max()
                .orElse(0.0);

        double minReactivePower = data.stream()
                .mapToDouble(PowerConsumption::getGlobalReactivePower)
                .min()
                .orElse(0.0);

        // Voltage statistics
        double avgVoltage = data.stream()
                .mapToDouble(PowerConsumption::getVoltage)
                .average()
                .orElse(0.0);

        double maxVoltage = data.stream()
                .mapToDouble(PowerConsumption::getVoltage)
                .max()
                .orElse(0.0);

        double minVoltage = data.stream()
                .mapToDouble(PowerConsumption::getVoltage)
                .min()
                .orElse(0.0);

        // Current Intensity statistics
        double avgIntensity = data.stream()
                .mapToDouble(PowerConsumption::getGlobalIntensity)
                .average()
                .orElse(0.0);

        double maxIntensity = data.stream()
                .mapToDouble(PowerConsumption::getGlobalIntensity)
                .max()
                .orElse(0.0);

        double minIntensity = data.stream()
                .mapToDouble(PowerConsumption::getGlobalIntensity)
                .min()
                .orElse(0.0);

        // Sub-metering statistics
        double totalSubMeter1 = data.stream()
                .mapToDouble(PowerConsumption::getSubMetering1)
                .sum();

        double totalSubMeter2 = data.stream()
                .mapToDouble(PowerConsumption::getSubMetering2)
                .sum();

        double totalSubMeter3 = data.stream()
                .mapToDouble(PowerConsumption::getSubMetering3)
                .sum();

        // Calculate "other" consumption (not measured by sub-meters)
        double totalOther = data.stream()
                .mapToDouble(d -> Math.max(0,
                        (d.getGlobalActivePower() * 1000 / 60) -
                                d.getSubMetering1() -
                                d.getSubMetering2() -
                                d.getSubMetering3()))
                .sum();

        double totalConsumption = totalSubMeter1 + totalSubMeter2 + totalSubMeter3 + totalOther;

        // Find peak usage
        PowerConsumption peakData = data.stream()
                .max(Comparator.comparing(PowerConsumption::getGlobalActivePower))
                .orElse(null);

        HouseStatistics.PeakUsageInfo peakUsage = null;
        if (peakData != null) {
            var peakTime = peakData.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDateTime();
            peakUsage = HouseStatistics.PeakUsageInfo.builder()
                    .timestamp(peakData.getTimestamp().toString())
                    .power(peakData.getGlobalActivePower())
                    .dayOfWeek(peakTime.getDayOfWeek().toString())
                    .hourOfDay(peakTime.getHour())
                    .build();
        }

        // Build statistics object
        return HouseStatistics.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalDataPoints((long) data.size())
                .period(period)
                .avgActivePower(avgActivePower)
                .maxActivePower(maxActivePower)
                .minActivePower(minActivePower)
                .totalActivePowerKwh(totalActivePowerKwh)
                .avgReactivePower(avgReactivePower)
                .maxReactivePower(maxReactivePower)
                .minReactivePower(minReactivePower)
                .avgVoltage(avgVoltage)
                .maxVoltage(maxVoltage)
                .minVoltage(minVoltage)
                .avgIntensity(avgIntensity)
                .maxIntensity(maxIntensity)
                .minIntensity(minIntensity)
                .kitchen(createSubMeteringStats("Kitchen", totalSubMeter1, totalConsumption))
                .laundry(createSubMeteringStats("Laundry", totalSubMeter2, totalConsumption))
                .hvac(createSubMeteringStats("HVAC", totalSubMeter3, totalConsumption))
                .other(createSubMeteringStats("Other", totalOther, totalConsumption))
                .estimatedCostUSD(totalActivePowerKwh * ELECTRICITY_RATE)
                .peakUsage(peakUsage)
                .build();
    }

    private HouseStatistics.SubMeteringStats createSubMeteringStats(
            String name,
            double totalWattHour,
            double totalConsumption) {

        double kwh = totalWattHour / 1000.0;
        double percentage = totalConsumption > 0 ? (totalWattHour / totalConsumption) * 100 : 0;

        return HouseStatistics.SubMeteringStats.builder()
                .name(name)
                .totalWattHour(totalWattHour)
                .totalKwh(kwh)
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
                .estimatedCostUSD(0.0)
                .build();
    }
}
