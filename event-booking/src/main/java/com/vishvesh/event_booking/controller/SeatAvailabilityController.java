package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.dto.seatavailability.SeatLockRequestDto;
import com.vishvesh.event_booking.repository.SeatAvailabilityRepository;
import com.vishvesh.event_booking.service.SeatAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/show/{showId}/seats")
@RequiredArgsConstructor
public class SeatAvailabilityController {
    private final SeatAvailabilityService seatAvailabilityService;

    @GetMapping("/getSeats")
    public ResponseEntity<Map<String, Object>> getAllSeats(@PathVariable UUID showId) {
        return ResponseEntity.status(200).body(seatAvailabilityService.getSeatsForShow(showId));
    }

    @PostMapping("/lock")
    public ResponseEntity<Map<String, Object>> lockSeats(
            @PathVariable UUID showId,
            @RequestBody SeatLockRequestDto request) {
        return ResponseEntity.ok(seatAvailabilityService.lockSeatsForShow(showId, request));
    }
}
