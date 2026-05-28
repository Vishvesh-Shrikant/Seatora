package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Screen;
import com.vishvesh.event_booking.entity.Show;
import com.vishvesh.event_booking.entity.Theatre;
import com.vishvesh.event_booking.repository.ScreenRepository;
import com.vishvesh.event_booking.repository.ShowRepository;
import com.vishvesh.event_booking.repository.TheatreRepository;
import com.vishvesh.event_booking.dto.theatre.TheatreResponseDto;
import com.vishvesh.event_booking.dto.theatre.TheatreRequestDto;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import com.vishvesh.event_booking.mapper.TheatreMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final ShowRepository showRepository;

    public Map<String , Object> getTheatres(String city)
    {
        // FIX 1.1: Always filter by isActive=true to prevent deactivated theatres leaking to users
        List<Theatre> theatresList = (city != null && !city.isBlank())
                ? theatreRepository.findByCityIgnoreCaseAndIsActiveTrue(city)
                : theatreRepository.findByIsActiveTrue();
        List<TheatreResponseDto> theatres= theatresList.stream()
                .map(TheatreMapper::mapToTheaterResponse)
                .toList();
        return Map.of("success", true,
                "message", "Theatres retrieved successfully" ,
                "count", theatres.size(),
                "theatres", theatres);
    }

    @Transactional
    public Map<String, Object> createTheater(TheatreRequestDto request) {
        Theatre theater = Theatre.builder()
                .name(request.getName())
                .city(request.getCity())
                .address(request.getAddress())
                .build();

        theater = theatreRepository.save(theater);
        log.info("New theater created: {} in {}", theater.getName(), theater.getCity());

        return Map.of(
                "success", true,
                "message", "Theater created successfully",
                "theater", TheatreMapper.mapToTheaterResponse(theater)
        );
    }

    @Transactional
    public Map<String, Object> updateTheater(UUID id, @NonNull TheatreRequestDto request) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Theater not found"));

        theatre.setName(request.getName());
        theatre.setCity(request.getCity());
        theatre.setAddress(request.getAddress());

        theatre = theatreRepository.save(theatre);
        log.info("Theater {} updated successfully", theatre.getId());

        return Map.of("success", true, "message", "Theater updated successfully", "theater", TheatreMapper.mapToTheaterResponse(theatre));
    }

    @Transactional
    public Map<String, Object> deactivateTheater(UUID id) {
        Theatre theater = theatreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Theater not found"));

        if (!theater.getIsActive()) {
            throw new IllegalStateException("Theater is already deactivated.");
        }

        // FIX 1.2: Cascade deactivation → cancel all SCHEDULED shows across all screens
        List<Screen> screens = screenRepository.findByTheaterId(theater.getId());
        for (Screen screen : screens) {
            List<Show> scheduledShows = showRepository.findByScreenIdAndShowStatus(
                    screen.getId(), ShowStatus.SCHEDULED);
            scheduledShows.forEach(show -> show.setShowStatus(ShowStatus.CANCELLED));
            showRepository.saveAll(scheduledShows);

            screen.setIsActive(false);
        }
        screenRepository.saveAll(screens);
        log.info("Cascade deactivated {} screen(s) and their scheduled shows under theatreId={}",
                screens.size(), id);

        theater.setIsActive(false);
        theatreRepository.save(theater);

        log.info("Theatre {} soft-deleted", id);
        return Map.of("success", true, "message", "Theater and all its screens/shows have been deactivated.");
    }

}
