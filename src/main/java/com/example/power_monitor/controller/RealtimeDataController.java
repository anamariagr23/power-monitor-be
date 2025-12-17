//package com.example.power_monitor.controller;
//
//import com.example.power_monitor.service.RealtimeSimulatorService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Controller;
//
//import java.time.LocalDate;
//import java.util.Map;
//
//@Slf4j
//@Controller
//@RequiredArgsConstructor
//public class RealtimeDataController {
//
//    private final RealtimeSimulatorService simulatorService;
//
//    @MessageMapping("/start")
//    public void startSimulation(@Payload Map<String, String> payload) {
//        String dateStr = payload.get("date");
//        log.info("WebSocket request to start simulation for date: {}", dateStr);
//
//        LocalDate date = LocalDate.parse(dateStr);
//        simulatorService.startSimulation(date);
//    }
//
//    @MessageMapping("/stop")
//    public void stopSimulation() {
//        log.info("WebSocket request to stop simulation");
//        simulatorService.stopSimulation();
//    }
//}
package com.example.power_monitor.controller;

import com.example.power_monitor.service.RealtimeSimulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RealtimeDataController {

    private final RealtimeSimulatorService simulatorService;

    @MessageMapping("/start")
    public void startSimulation(@Payload Map<String, String> payload) {
        String dateStr = payload.get("date");
        log.info("▶️ WebSocket request to start simulation (date parameter: {})", dateStr);
        log.info("   Note: Date is accepted but simulation will use ALL CSV data");

        // Pass date string (will be ignored by service, but keeps API compatible)
        simulatorService.startSimulation(dateStr);
    }

    @MessageMapping("/stop")
    public void stopSimulation() {
        log.info("⏹️ WebSocket request to stop simulation");
        simulatorService.stopSimulation();
    }
}
