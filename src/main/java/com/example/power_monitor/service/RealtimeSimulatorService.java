//package com.example.power_monitor.service;
//
//import com.example.power_monitor.model.PowerConsumption;
//import com.example.power_monitor.repository.CsvDataRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Iterator;
//import java.util.List;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RealtimeSimulatorService {
//
//    private final SimpMessagingTemplate messagingTemplate;
//    private final CsvDataRepository csvDataRepository;
//
//    private ScheduledExecutorService scheduler;
//    private Iterator<PowerConsumption> dataIterator;
//    private boolean isRunning = false;
//
//    public void startSimulation(LocalDate date) {
//        if (isRunning) {
//            log.warn("Simulation already running, stopping previous simulation");
//            stopSimulation();
//        }
//
//        log.info("Starting realtime simulation for date: {}", date);
//
//        List<PowerConsumption> data = csvDataRepository.getDataForDate(date);
//
//        if (data.isEmpty()) {
//            log.error("No data found for date: {}", date);
//            return;
//        }
//
//        dataIterator = data.iterator();
//        scheduler = Executors.newScheduledThreadPool(1);
//        isRunning = true;
//
//        // Send first data point immediately, then every minute
//        scheduler.scheduleAtFixedRate(
//                this::sendNextDataPoint,
//                0,      // initial delay
//                60,     // period (change to 1 for every second during testing)
//                TimeUnit.SECONDS
//        );
//
//        log.info("Realtime simulation started with {} data points", data.size());
//    }
//
//    private void sendNextDataPoint() {
//        if (!dataIterator.hasNext()) {
//            log.info("Reached end of data, restarting from beginning");
//            // Optionally restart or stop
//            stopSimulation();
//            return;
//        }
//
//        PowerConsumption data = dataIterator.next();
//
//        // Update timestamp to current time for realism
////        data.setTimestamp(LocalDateTime.now());
////        data.setIsRealtime(true);
//
//        PowerConsumption originalData = dataIterator.next();
//
//// ✅ Create NEW object with current timestamp + CSV values
//        PowerConsumption realtimeData = PowerConsumption.builder()
//                .timestamp(Instant.now())  // ✅ Current time with timezone!
//                .globalActivePower(originalData.getGlobalActivePower())  // CSV value
//                .voltage(originalData.getVoltage())                      // CSV value
//                .globalIntensity(originalData.getGlobalIntensity())      // CSV value
//                .globalReactivePower(originalData.getGlobalReactivePower()) // CSV value
//                .subMetering1(originalData.getSubMetering1())
//                .subMetering2(originalData.getSubMetering2())
//                .subMetering3(originalData.getSubMetering3())
//                .build();
//        log.info(
//                "📡 Sending realtime data → destination=/topic/realtime, time={}, GAP={}",
//                data.getTimestamp(),
//                data.getGlobalActivePower()
//        );
//
//        messagingTemplate.convertAndSend("/topic/realtime", realtimeData);
////        messagingTemplate.convertAndSend("/topic/realtime", data);
//    }
//
//    public void stopSimulation() {
//        if (scheduler != null && !scheduler.isShutdown()) {
//            log.info("Stopping realtime simulation");
//            scheduler.shutdown();
//            try {
//                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
//                    scheduler.shutdownNow();
//                }
//            } catch (InterruptedException e) {
//                scheduler.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
//        }
//        isRunning = false;
//    }
//
//    public boolean isRunning() {
//        return isRunning;
//    }
//}

package com.example.power_monitor.service;

import com.example.power_monitor.model.PowerConsumption;
import com.example.power_monitor.repository.CsvDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeSimulatorService {

    private final SimpMessagingTemplate messagingTemplate;
    private final CsvDataRepository csvDataRepository;

    private ScheduledExecutorService scheduler;
    private List<PowerConsumption> sourceData;
    private Iterator<PowerConsumption> dataIterator;
    private boolean isRunning = false;
    private int dataPointsSent = 0;

    /**
     * ✅ FIXED: Now uses ALL CSV data, ignores date parameter
     */
    public void startSimulation(String dateStr) {
        if (isRunning) {
            log.warn("Simulation already running, stopping previous simulation");
            stopSimulation();
        }

        log.info("🚀 Starting realtime simulation");
        log.info("   Note: Using ALL CSV data (date parameter ignored)");

        // ✅ Get ALL data from CSV
        sourceData = csvDataRepository.getAllData();

        if (sourceData.isEmpty()) {
            log.error("❌ No data available in CSV");
            return;
        }

        dataIterator = sourceData.iterator();
        scheduler = Executors.newScheduledThreadPool(1);
        isRunning = true;
        dataPointsSent = 0;

        // Send data every 60 seconds
        scheduler.scheduleAtFixedRate(
                this::sendNextDataPoint,
                0,
                60,
                TimeUnit.SECONDS
        );

        log.info("✅ Realtime simulation started");
        log.info("   Total data points available: {}", sourceData.size());
        log.info("   Sending interval: 1 second");
        log.info("   First value: {} kW", sourceData.get(0).getGlobalActivePower());
    }

    private void sendNextDataPoint() {
        try {
            if (!isRunning) {
                return;
            }

            // ✅ Restart from beginning when we reach the end
            if (!dataIterator.hasNext()) {
                log.info("🔄 Reached end of data, restarting from beginning");
                dataIterator = sourceData.iterator();
            }

            PowerConsumption originalData = dataIterator.next();

            // ✅ Create NEW object with CURRENT timestamp + CSV values
            PowerConsumption realtimeData = PowerConsumption.builder()
                    .timestamp(Instant.now())  // ✅ TODAY's timestamp!
                    .globalActivePower(originalData.getGlobalActivePower())
                    .globalReactivePower(originalData.getGlobalReactivePower())
                    .voltage(originalData.getVoltage())
                    .globalIntensity(originalData.getGlobalIntensity())
                    .subMetering1(originalData.getSubMetering1())
                    .subMetering2(originalData.getSubMetering2())
                    .subMetering3(originalData.getSubMetering3())
                    .isRealtime(true)
                    .build();

            dataPointsSent++;

            // Send to WebSocket
            messagingTemplate.convertAndSend("/topic/realtime", realtimeData);

            // Log every 10th point to avoid spam
//            if (dataPointsSent % 10 == 0) {
            if (dataPointsSent % 2 == 0) {
                log.info(
                        "📡 Sent #{}: {} | Power: {:.3f} kW | Voltage: {:.2f} V",
                        dataPointsSent,
                        DateTimeFormatter.ofPattern("HH:mm:ss").format(
                                realtimeData.getTimestamp().atZone(ZoneId.systemDefault())
                        ),
                        realtimeData.getGlobalActivePower(),
                        realtimeData.getVoltage()
                );
            }

        } catch (Exception e) {
            log.error("❌ Error sending realtime data: {}", e.getMessage(), e);
        }
    }

    public void stopSimulation() {
        if (scheduler != null && !scheduler.isShutdown()) {
            log.info("⏹️ Stopping realtime simulation");
            log.info("   Total data points sent: {}", dataPointsSent);

            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        isRunning = false;
        dataPointsSent = 0;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getDataPointsSent() {
        return dataPointsSent;
    }
}
