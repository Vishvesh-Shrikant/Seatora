package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.service.TheatreService;
import com.vishvesh.event_booking.dto.theatre.TheatreRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(("/api/theatre"))
@RequiredArgsConstructor
public class TheatreController {

    private final TheatreService theatreService;

    @GetMapping("/getTheatres")
    public ResponseEntity<Map<String, Object>> getTheatres(@RequestParam String city) {
        Map<String, Object> response = theatreService.getTheatres(city);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/createTheatre")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Map<String, Object>> createTheatre (@RequestBody TheatreRequestDto request){
        Map<String, Object> response= theatreService.createTheater(request);
        return ResponseEntity.status(201).body(response);
    }

    @PatchMapping("/updateTheatre/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateTheatre(@PathVariable UUID id, @RequestBody TheatreRequestDto request){
        Map<String, Object> response= theatreService.updateTheater(id,  request);
        return ResponseEntity.status(200).body(response);
    }

    @DeleteMapping("/deactivateTheatre/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateTheatre(@PathVariable UUID id){
        Map<String, Object> response = theatreService.deactivateTheater(id);
        return ResponseEntity.status(204).body(response);
    }




}

