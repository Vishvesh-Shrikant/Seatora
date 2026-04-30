package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.service.SeatService;
import com.vishvesh.event_booking.dto.seat.SeatRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;

    @GetMapping("/getScreenSeats/{seatID}")
    public ResponseEntity<Map<String, Object>> getAllSeatsForScreen(@PathVariable UUID screenId) {
        return ResponseEntity.status(200).body(seatService.getAllSeatsForScreen(screenId));
    }

    @PostMapping("/addSeatToScreen")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> addSeat(@RequestBody SeatRequestDto request) {
        return ResponseEntity.status(201).body(seatService.addSeat(request.getScreenId(), request));
    }

    @PatchMapping("/updateSeat/{seatId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>>  updateSeat(@PathVariable UUID seatId, @RequestBody SeatRequestDto request) {
        return ResponseEntity.status(200).body(seatService.updateSeat(seatId, request));
    }

    @DeleteMapping("deactivateSeat/{seatId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateSeat(@PathVariable UUID seatId) {
        return ResponseEntity.status(200).body(seatService.deactivateSeat(seatId));
    }

}
