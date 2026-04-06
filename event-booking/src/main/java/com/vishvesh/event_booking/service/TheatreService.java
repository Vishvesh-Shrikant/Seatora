package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Theatre;
import com.vishvesh.event_booking.repository.ScreenRepository;
import com.vishvesh.event_booking.repository.TheatreRepository;
import com.vishvesh.event_booking.dto.theatre.TheatreReponseDto;
import com.vishvesh.event_booking.dto.theatre.TheatreRequestDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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

    public Map<String , Object> getTheatres(String city)
    {
        List<Theatre> theatresList = (city != null && !city.isBlank())
                ? theatreRepository.findByCityIgnoreCaseAndIsActiveTrue(city)
                : theatreRepository.findAll();
        List<TheatreReponseDto> theatres= theatresList.stream()
                .map(this::mapToTheaterResponse )
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
                "theater", mapToTheaterResponse(theater)
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

        return Map.of("success", true, "message", "Theater updated successfully", "theater", mapToTheaterResponse(theatre));
    }

    @Transactional
    public Map<String, Object> deactivateTheater(UUID id) {
        Theatre theater = theatreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Theater not found"));

        if (!theater.getIsActive()) {
            throw new IllegalStateException("Theater is already deleted.");
        }
        theater.setIsActive(false);
        theatreRepository.save(theater);

        log.info("Theater {} soft-deleted", id);

        return Map.of("success", true, "message", "Theater successfully deactivated and hidden from users.");
    }


    private TheatreReponseDto mapToTheaterResponse(@NonNull Theatre theatre) {
        return TheatreReponseDto.builder()
                .theatreId(theatre.getId())
                .theatreName(theatre.getName())
                .theatreCity(theatre.getCity())
                .theatreAddress(theatre.getAddress())
                .build();
    }



}
