package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.dto.show.ShowRequestDto;
import com.vishvesh.event_booking.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping("/availableShowsForMovie/{movieId}")
    public ResponseEntity<Map<String, Object>> getAllAvailableShowsForMovie(@PathVariable UUID movieId, @RequestParam LocalDate date){
        return ResponseEntity.status(200).body(showService.getAvailableShowsOfMoviesForDate(movieId, date));
    }

    @GetMapping("/{showId}")
    public ResponseEntity<Map<String, Object>> getShowById(@PathVariable UUID showId) {
        return ResponseEntity.status(200).body(showService.getShowById(showId));
    }

    @GetMapping("/allShowsForMovie/{movieId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllShowsForMovie(@PathVariable UUID movieId){
        return ResponseEntity.status(200).body(showService.getAllShowsByMovie(movieId));
    }

    @GetMapping("/allShowsForScreen/{screenId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllShowsForScreen(@PathVariable UUID screenId){
        return ResponseEntity.status(200).body(showService.getAllShowsByScreen(screenId));
    }

    @GetMapping("/allShowsBetweenDates")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllShowsBetweenDates(@RequestParam LocalDate startDate, LocalDate endDate){
        return ResponseEntity.status(200).body(showService.getAllShowsBetweenDates(startDate, endDate));
    }

    @GetMapping("/getShowByStatus")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getShowByStatus(@RequestParam String status){
        return ResponseEntity.status(200).body(showService.getAllShowsByShowStatus(status));
    }

    @PostMapping("/addNewShow")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> addNewShow(@RequestBody ShowRequestDto show){
        return ResponseEntity.status(201).body(showService.addNewShow(show));
    }

    @PatchMapping("/updateShow/{showId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateShow(@PathVariable UUID showId, @RequestBody ShowRequestDto show){
        return ResponseEntity.status(200).body(showService.updateShow(showId, show));
    }

    @DeleteMapping("/cancelShow/{showId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelShow(@PathVariable UUID showId){
        return ResponseEntity.status(200).body(showService.cancelShow(showId));
    }
}
