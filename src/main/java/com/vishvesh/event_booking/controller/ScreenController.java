package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.service.ScreenService;
import com.vishvesh.event_booking.dto.screen.ScreenRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/screen")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    @PostMapping("/createScreen")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> createScreen(@RequestBody ScreenRequestDto screen) {
        return ResponseEntity.status(201).body(screenService.createScreen(screen));
    }

    @GetMapping("/getTheatreScreens/{theatreId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTheatreScreens(@PathVariable UUID theatreID) {
        return ResponseEntity.status(200).body(screenService.getScreensByTheater(theatreID));
    }

    @PatchMapping("/updateScreen/{screenId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateScreen(@PathVariable UUID screenId, @RequestBody ScreenRequestDto screen) {
        return ResponseEntity.status(200).body(screenService.updateScreen(screenId, screen));
    }

    @DeleteMapping("/deactivateScreen/{screenId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateScreen(@PathVariable UUID screenId) {
        return ResponseEntity.status(204).body(screenService.deactivateScreen(screenId));
    }
}
