//package com.example.power_monitor.repository;
//
//import com.example.power_monitor.model.PowerConsumption;
//import com.opencsv.CSVParserBuilder;
//import com.opencsv.CSVReader;
//import com.opencsv.CSVReaderBuilder;
//import com.opencsv.exceptions.CsvException;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.stereotype.Repository;
//
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Slf4j
//@Repository
//public class CsvDataRepository {
//
//    @Value("${data.csv.path}")
//    private String csvFilePath;
//
//    private final Map<LocalDate, List<PowerConsumption>> dataCache = new ConcurrentHashMap<>();
//
//    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
//
//
//    @PostConstruct
//    public void loadDataFromCsv() {
//        log.info("=== STARTING CSV LOAD ===");
//        log.info("CSV Path from config: {}", csvFilePath);
//
//        try {
//            ClassPathResource resource = new ClassPathResource("data/power-sample.csv");
//
//            // ✅ ADD THESE DEBUG LINES
//            log.info("Resource exists: {}", resource.exists());
//            log.info("Resource filename: {}", resource.getFilename());
//            log.info("Resource path: {}", resource.getPath());
//
//            if (!resource.exists()) {
//                log.error("CSV FILE NOT FOUND! Check src/main/resources/data/power-sample.csv");
//                return;
//            }
//
////            CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()));
//            CSVReader reader = new CSVReaderBuilder(
//                    new InputStreamReader(resource.getInputStream())
//            ).withCSVParser(
//                    new CSVParserBuilder()
//                            .withSeparator(';')
//                            .build()
//            ).build();
//            List<String[]> allRows = reader.readAll();
//
//            // ✅ ADD THIS DEBUG LINE
//            log.info("Total rows in CSV (including header): {}", allRows.size());
//
//            if (allRows.size() <= 1) {
//                log.error("CSV file is empty or only has header!");
//                return;
//            }
//
//            // ✅ ADD THIS DEBUG LINE - Show first data row
//            if (allRows.size() > 1) {
//                log.info("First data row: {}", String.join(";", allRows.get(1)));
//            }
//
//            int successCount = 0;
//            int failCount = 0;
//
//            // Skip header row
//            for (int i = 1; i < allRows.size(); i++) {
//                String[] row = allRows.get(i);
//
//                try {
//                    PowerConsumption data = parseRow(row);
//                    LocalDate date = data.getTimestamp().toLocalDate();
//
//                    dataCache.computeIfAbsent(date, k -> new ArrayList<>()).add(data);
//                    successCount++;
//
//                    // ✅ ADD THIS DEBUG LINE - Show first few parsed dates
//                    if (i <= 5) {
//                        log.info("Row {}: Parsed date = {}, timestamp = {}", i, date, data.getTimestamp());
//                    }
//
//                } catch (Exception e) {
//                    failCount++;
//                    log.warn("Failed to parse row {}: {} - Data: {}", i, e.getMessage(), String.join(";", row));
//                    // ✅ ADD THIS to see full error
//                    if (failCount <= 3) {
//                        log.error("Parse error details:", e);
//                    }
//                }
//            }
//
//            log.info("=== CSV LOAD COMPLETE ===");
//            log.info("Successfully parsed: {} rows", successCount);
//            log.info("Failed to parse: {} rows", failCount);
//            log.info("Loaded data for {} unique dates", dataCache.size());
//
//            // ✅ ADD THIS - Show all dates loaded
//            dataCache.forEach((date, data) ->
//                    log.info("  ✓ Date: {} ({}) - {} records", date, date.format(DateTimeFormatter.ISO_LOCAL_DATE), data.size())
//            );
//
//        } catch (IOException | CsvException e) {
//            log.error("=== CSV LOAD FAILED ===");
//            log.error("Error loading CSV data", e);
//            e.printStackTrace();
//            throw new RuntimeException("Failed to load CSV data", e);
//        }
//    }
//
//    private PowerConsumption parseRow(String[] row) {
//        String dateStr = row[0];
//        String timeStr = row[1];
//
//        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
//        LocalDateTime timestamp = date.atTime(
//                Integer.parseInt(timeStr.substring(0, 2)),
//                Integer.parseInt(timeStr.substring(3, 5)),
//                Integer.parseInt(timeStr.substring(6, 8))
//        );
//
//        return new PowerConsumption(
//                timestamp,
//                parseDouble(row[2]),
//                parseDouble(row[3]),
//                parseDouble(row[4]),
//                parseDouble(row[5]),
//                parseDouble(row[6]),
//                parseDouble(row[7]),
//                parseDouble(row[8])
//        );
//    }
//
//    private Double parseDouble(String value) {
//        try {
//            return value == null || value.trim().isEmpty() || value.equals("?")
//                    ? 0.0
//                    : Double.parseDouble(value);
//        } catch (NumberFormatException e) {
//            return 0.0;
//        }
//    }
//
//    public List<PowerConsumption> getDataForDate(LocalDate date) {
//        // ✅ ADD DEBUG LOGGING HERE TOO
//        log.info("Looking for data for date: {}", date);
//        log.info("Available dates in cache: {}", dataCache.keySet());
//
//        List<PowerConsumption> data = dataCache.getOrDefault(date, new ArrayList<>());
//        log.info("Found {} records for date {}", data.size(), date);
//
//        return data;
//    }
//
//    public List<LocalDate> getAvailableDates() {
//        return new ArrayList<>(dataCache.keySet());
//    }
//}

package com.example.power_monitor.repository;

import com.example.power_monitor.model.PowerConsumption;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class CsvDataRepository {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String CSV_PATH = "data/power-sample.csv";

    // ✅ Cache ALL data on startup
    private List<PowerConsumption> allData = new ArrayList<>();

    @PostConstruct
    public void init() {
        log.info("🔄 Loading CSV data on startup...");
        allData = loadAllData();
        log.info("✅ Loaded {} total data points from CSV", allData.size());

        if (!allData.isEmpty()) {
            log.info("   First timestamp: {}", allData.get(0).getTimestamp());
            log.info("   Last timestamp: {}", allData.get(allData.size() - 1).getTimestamp());
        }
    }

    /**
     * ✅ NEW: Get ALL data (for real-time simulation)
     */
    public List<PowerConsumption> getAllData() {
        log.info("📊 Returning ALL {} data points for simulation", allData.size());
        return new ArrayList<>(allData); // Return copy
    }

    /**
     * Get data for a SPECIFIC date (for historical API)
     */
    public List<PowerConsumption> getDataForDate(LocalDate targetDate) {
        log.info("📅 Filtering data for date: {}", targetDate);

        List<PowerConsumption> results = allData.stream()
                .filter(data -> {
                    LocalDate dataDate = data.getTimestamp()
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate();
                    return dataDate.equals(targetDate);
                })
                .collect(Collectors.toList());

        log.info("✅ Found {} data points for date {}", results.size(), targetDate);
        return results;
    }

    /**
     * Get available dates (for historical API)
     */
    public List<LocalDate> getAvailableDates() {
        return allData.stream()
                .map(data -> data.getTimestamp().atZone(ZoneId.of("UTC")).toLocalDate())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Load all data from CSV file
     */
    private List<PowerConsumption> loadAllData() {
        List<PowerConsumption> results = new ArrayList<>();

        try {
            String path;
            try {
                path = new ClassPathResource(CSV_PATH).getFile().getAbsolutePath();
            } catch (IOException e) {
                path = "src/main/resources/" + CSV_PATH;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String line = br.readLine(); // Skip header

                log.info("📖 Reading CSV file: {}", path);

                int lineNumber = 0;
                int validCount = 0;

                while ((line = br.readLine()) != null) {
                    lineNumber++;

                    if (line.trim().isEmpty() || line.contains("?")) {
                        continue;
                    }

                    try {
                        String[] parts = line.split(";");

                        if (parts.length < 7) {
                            continue;
                        }

                        // Parse date and time from CSV
                        LocalDate csvDate = LocalDate.parse(parts[0].trim(), DATE_FORMATTER);
                        LocalTime csvTime = LocalTime.parse(parts[1].trim(), TIME_FORMATTER);
                        LocalDateTime dateTime = LocalDateTime.of(csvDate, csvTime);
                        Instant timestamp = dateTime.atZone(ZoneId.of("UTC")).toInstant();

                        PowerConsumption data = PowerConsumption.builder()
                                .timestamp(timestamp)
                                .globalActivePower(parseDouble(parts[2]))
                                .voltage(parseDouble(parts[4]))
                                .globalIntensity(parseDouble(parts[5]))
                                .globalReactivePower(parseDouble(parts[3]))
                                .subMetering1(parseDouble(parts[6]))
                                .subMetering2(parts.length > 7 ? parseDouble(parts[7]) : 0.0)
                                .subMetering3(parts.length > 8 ? parseDouble(parts[8]) : 0.0)
                                .build();

                        results.add(data);
                        validCount++;

                    } catch (Exception e) {
                        // Skip invalid lines silently
                    }
                }

                log.info("✅ Parsed {} valid lines out of {} total", validCount, lineNumber);

            }
        } catch (IOException e) {
            log.error("❌ Error reading CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to read CSV file", e);
        }

        return results;
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty() || "?".equals(value)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public long getDataCountForDate(LocalDate date) {
        return getDataForDate(date).size();
    }

    public long getTotalDataCount() {
        return allData.size();
    }
}
