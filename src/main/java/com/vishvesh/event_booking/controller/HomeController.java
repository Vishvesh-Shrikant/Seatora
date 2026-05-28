package com.vishvesh.event_booking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>>health() {
        return  ResponseEntity.status(200).body(Map.of(
                "success", true,
                "message", "Server is healthy and running..."
        ));
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        return ResponseEntity.status(200).body(Map.of("message", "Welcome to the server port 8080..."));
    }
}
