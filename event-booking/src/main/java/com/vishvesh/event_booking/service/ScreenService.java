package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Screen;
import com.vishvesh.event_booking.entity.Show;
import com.vishvesh.event_booking.entity.Theatre;
import com.vishvesh.event_booking.repository.ScreenRepository;
import com.vishvesh.event_booking.repository.ShowRepository;
import com.vishvesh.event_booking.repository.TheatreRepository;
import com.vishvesh.event_booking.dto.screen.ScreenRequestDto;
import com.vishvesh.event_booking.dto.screen.ScreenResponseDto;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import com.vishvesh.event_booking.mapper.ScreenMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final ShowRepository showRepository;

    @Transactional
    public Map<String, Object> createScreen(@NonNull ScreenRequestDto request) {
        Theatre theatre = theatreRepository.findById(request.getTheatreId())
                .orElseThrow(() -> new IllegalArgumentException("Theater not found"));

        if(!theatre.getIsActive()){
            throw new IllegalStateException("The given theatre is not active");
        }
        if (screenRepository.existsByTheaterIdAndScreenNoAndIsActiveTrue(theatre.getId(), request.getScreenNo())) {
            throw new IllegalStateException("Screen " + request.getScreenNo()+ " already exists and is active in this theater");
        }

        Screen screen = Screen.builder()
                .theater(theatre)
                .screenNo(request.getScreenNo())
                .totalSeats(request.getTotalSeats())
                .build();

        screen = screenRepository.save(screen);
        log.info("Added Screen {} to Theater {}", screen.getScreenNo(), theatre.getName());
        return Map.of("success", true, "message", "Screen added successfully", "screen", ScreenMapper.mapToScreenResponse(screen));
    }

    public Map<String, Object> getScreensByTheater(UUID theaterId) {
        if (!theatreRepository.existsById(theaterId)) {
            throw new IllegalArgumentException("Theater not found");
        }
        List<ScreenResponseDto> screens= screenRepository.findByTheaterIdAndIsActiveTrue(theaterId)
                .stream().map(ScreenMapper::mapToScreenResponse).toList();

        return Map.of("success", true, "message", "Screens retrieved successfully", "screens", screens);
    }

    @Transactional
    public Map<String, Object> updateScreen( UUID id, ScreenRequestDto request) {
        Screen screen = screenRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Screen not found"));

        if (!screen.getIsActive()) {
            throw new IllegalStateException("Cannot update a deactivated screen.");
        }

        if (!screen.getScreenNo().equals(request.getScreenNo()) &&
                screenRepository.existsByTheaterIdAndScreenNoAndIsActiveTrue(screen.getTheater().getId(), request.getScreenNo())) {
            throw new IllegalStateException("Screen " + request.getScreenNo() + " already exists in this theater");
        }

        screen.setScreenNo(request.getScreenNo());
        screen.setTotalSeats(request.getTotalSeats());

        screen = screenRepository.save(screen);
        log.info("Screen {} updated", screen.getScreenNo());
        return Map.of("success", true, "message", "Screen updated successfully", "screen", screen);
    }

    @Transactional
    public Map<String, Object> deactivateScreen(UUID screenID) {
        Screen screen = screenRepository.findById(screenID)
                .orElseThrow(() -> new IllegalArgumentException("Screen not found"));

        if (!screen.getIsActive()) {
            throw new IllegalStateException("Screen is already deactivated.");
        }

        // FIX 1.2: Cancel all SCHEDULED shows on this screen before deactivating it
        List<Show> scheduledShows = showRepository.findByScreenIdAndShowStatus(
                screenID, ShowStatus.SCHEDULED);
        scheduledShows.forEach(show -> show.setShowStatus(ShowStatus.CANCELLED));
        showRepository.saveAll(scheduledShows);
        log.info("Cancelled {} scheduled show(s) for screenId={}", scheduledShows.size(), screenID);

        screen.setIsActive(false);
        screenRepository.save(screen);

        log.info("Screen {} deactivated", screen.getScreenNo());
        return Map.of("success", true, "message", "Screen deactivated and all scheduled shows cancelled.");
    }

}
