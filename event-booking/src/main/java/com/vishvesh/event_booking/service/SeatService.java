package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Screen;
import com.vishvesh.event_booking.entity.Seat;
import com.vishvesh.event_booking.repository.ScreenRepository;
import com.vishvesh.event_booking.repository.SeatRepository;
import com.vishvesh.event_booking.dto.seat.SeatRequestDto;
import com.vishvesh.event_booking.dto.seat.SeatResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    public Map<String, Object> getAllSeatsForScreen(UUID screenId){
        if(screenRepository.findById(screenId).isEmpty()){
            throw new IllegalStateException("No screen found with id: " + screenId);
        }
        List<Seat> seatsByScreen = seatRepository.findByScreenIdAndIsActiveTrue(screenId);

        return Map.of("success", true, "message", "Seats for given screen retrieved successfully", "seats", seatsByScreen.stream().map(this::mapToSeatResponseDto).toList());
    }

    public Map<String, Object> addSeat(UUID screenId, SeatRequestDto seatRequestDto){
        Screen screen = screenRepository.findById(screenId).get();
        if(screenRepository.findById(screenId).isEmpty()){
            throw new IllegalStateException("No screen found with id: " + screenId);
        }
        if(!screen.getIsActive())
        {
            throw new IllegalStateException("Screen is not active.");
        }
        if(seatRepository.existsByIdAndSeatNoAndIsActiveTrue(screenId,  seatRequestDto.getSeatNo()))
        {
            throw new IllegalStateException("Seat is already active.");
        }
        int currentSeatCount = seatRepository.countByScreenIdAndIsActiveTrue(screen.getId());
        if (currentSeatCount >= screen.getTotalSeats()) {
            throw new IllegalStateException("Screen capacity reached. Cannot add more than " + screen.getTotalSeats() + " seats.");
        }

        Seat seat = Seat.builder()
                .seatNo(seatRequestDto.getSeatNo())
                .seatType(seatRequestDto.getSeatType())
                .screen(screen)
                .build();
        seatRepository.save(seat);
        log.info("Seat {} added to Screen {}", seat.getSeatNo(), screen.getScreenNo());
        return Map.of("success", true, "message", "Seats for given screen retrieved successfully",  "seats", mapToSeatResponseDto(seat));
    }

    public Map<String , Object> updateSeat(UUID seatId, SeatRequestDto seatRequestDto){
        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new IllegalArgumentException("Screen not found"));

        if(!seat.getIsActive())
        {
            throw new IllegalStateException("Seat is not active.");
        }
        if(!seat.getSeatNo().equals(seatRequestDto.getSeatNo()) && seatRepository.existsByIdAndSeatNoAndIsActiveTrue(seatId, seatRequestDto.getSeatNo()))
        {
            throw new IllegalStateException("Seat with seat name already exists.");
        }

        seat.setSeatNo(seatRequestDto.getSeatNo());
        seat.setSeatType(seatRequestDto.getSeatType());

        seat = seatRepository.save(seat);


        return Map.of("success", true, "message", "Seats for given screen retrieved successfully",  "seats", mapToSeatResponseDto(seat));
    }

    public Map<String, Object> deactivateSeat(UUID seatId){
        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new IllegalArgumentException("Screen not found"));

        if(!seat.getIsActive())
        {
            throw new IllegalStateException("Seat is already deactivated.");
        }
        seat.setIsActive(false);
        Seat deletedSeat = seatRepository.save(seat);

        log.info("Seat {} deactivated", seatId);
        return Map.of("success", true, "message", "Seats for given screen retrieved successfully",  "seats", mapToSeatResponseDto(deletedSeat));
    }

    private SeatResponseDto mapToSeatResponseDto(Seat seat){
        return SeatResponseDto.builder()
                .seatId(seat.getId())
                .seatNo(seat.getSeatNo())
                .screenId(seat.getScreen().getId())
                .seatType(seat.getSeatType())
                .build();
    }
}
