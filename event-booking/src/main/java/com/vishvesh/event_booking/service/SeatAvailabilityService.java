package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.dto.seatavailability.SeatAvailabilityResponseDto;
import com.vishvesh.event_booking.dto.seatavailability.SeatLockRequestDto;
import com.vishvesh.event_booking.entity.SeatAvailability;
import com.vishvesh.event_booking.entity.Show;
import com.vishvesh.event_booking.entity.User;
import com.vishvesh.event_booking.repository.*;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatAvailabilityService {

    private final SeatAvailabilityRepository seatAvailabilityRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final UserPenaltyRepository  userPenaltyRepository;

    private static final int LOCK_DURATION_MINS= 15;
    private static final int MAX_SEATS_PER_REQUEST = 6;

    public Map<String, Object> getSeatsForShow(UUID showId) {
        if (!showRepository.existsById(showId)) {
            throw new IllegalStateException("Show does not exist");
        }
        List<SeatAvailability> seats = seatAvailabilityRepository.findByShowId(showId);

        return Map.of(
                "success", true,
                "message", "Seats retrieved successfully",
                "seats", seats.stream().map(this::mapToSeatAvailabilityDto).toList()
        );
    }

    @Transactional
    public Map<String, Object> lockSeatsForShow(UUID showId, SeatLockRequestDto request) {

        if (request.getSeatIds().size() > MAX_SEATS_PER_REQUEST) {
            throw new IllegalStateException("You can only book up to " + MAX_SEATS_PER_REQUEST + " seats at a time.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new IllegalStateException("Show not found"));
        if (show.getShowStatus() != ShowStatus.SCHEDULED) {
            throw new IllegalStateException("Tickets cannot be booked for a " + show.getShowStatus() + " show.");
        }


        boolean isPenalized = userPenaltyRepository.existsByUserIdAndShowIdAndPenaltyExpiryAfter(
                request.getUserId(), showId, OffsetDateTime.now()
        );

        if (isPenalized) {
            throw new IllegalStateException("You recently let a reservation expire. Please wait 15 minutes before trying to book seats for this show again.");
        }
        
        List<SeatAvailability> previousLocks = seatAvailabilityRepository
                .findByShowIdAndLockedByIdAndSeatStatus(showId, user.getUserId(), SeatStatus.LOCKED);
        if (!previousLocks.isEmpty()) {
            previousLocks.forEach(seat -> {
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seat.setLockedBy(null);
                seat.setLockedAt(null);
                seat.setLockExpiry(null);
            });
            seatAvailabilityRepository.saveAll(previousLocks);
        }

        List<SeatAvailability> targetSeats = seatAvailabilityRepository
                .findByShowIdAndSeatIdIn(showId, request.getSeatIds());

        OffsetDateTime now = OffsetDateTime.now();

        if (targetSeats.size() != request.getSeatIds().size()) {
            throw new IllegalStateException("One or more seats do not exist for this show");
        }
        OffsetDateTime expiryTime = now.plusMinutes(LOCK_DURATION_MINS);

        targetSeats.forEach(seat -> {
            seat.setSeatStatus(SeatStatus.LOCKED);
            seat.setLockedBy(user);
            seat.setLockedAt(OffsetDateTime.now());
            seat.setLockExpiry(expiryTime);
        });

        List<SeatAvailability> lockedSeats = seatAvailabilityRepository.saveAll(targetSeats);
        return Map.of(
                "success", true,
                "message", "Seats retrieved successfully",
                "seats", lockedSeats.stream().map(this::mapToSeatAvailabilityDto).toList()
        );
    }

    private SeatAvailabilityResponseDto mapToSeatAvailabilityDto(@NonNull SeatAvailability seat){
        return SeatAvailabilityResponseDto.builder()
                .id(seat.getId())
                .showId(seat.getShow().getId())
                .seatStatus(seat.getSeatStatus())
                .seatId(seat.getSeat().getId())
                .lockedByUserId(seat.getLockedBy().getUserId())
                .build();
    }
}
