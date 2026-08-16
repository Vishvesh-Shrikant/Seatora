package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.service.MovieService;
import com.vishvesh.event_booking.dto.movie.MovieRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/movie")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/getMovies")
    public ResponseEntity<Map<String, Object>> getMovies(@RequestParam String searchTitle) {
        return ResponseEntity.status(200).body(movieService.getAllMovies(searchTitle));
    }

    @PostMapping("/addMovie")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> addMovie(@RequestBody MovieRequestDto movieRequest){
        return ResponseEntity.status(201).body(movieService.addMovie(movieRequest));
    }

    @PatchMapping("/updateMovie/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateMovie (@PathVariable UUID id, @RequestBody MovieRequestDto movieRequestDto){
        return ResponseEntity.status(200).body(movieService.updateMovie(id, movieRequestDto));
    }

    @DeleteMapping("/removeMovie/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeMovie(@PathVariable UUID id){
        return  ResponseEntity.status(200).body(movieService.removeMovie(id));
    }
}
