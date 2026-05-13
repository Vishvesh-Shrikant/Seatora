package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.dto.seatavailability.SeatLockRequestDto;
import com.vishvesh.event_booking.entity.*;
import com.vishvesh.event_booking.repository.*;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.vishvesh.event_booking.mapper.SeatAvailabilityMapper;
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
    private final UserPenaltyRepository userPenaltyRepository;
    private final BookingRepository bookingRepository;

    private static final int LOCK_DURATION_MINS = 10; // Aligned with booking expiry window in BookingService
    private static final int MAX_SEATS_PER_REQUEST = 6;
    private static final int PENALTY_BLOCK_THRESHOLD = 3; // Block user only after 3 consecutive abandons

    public Map<String, Object> getSeatsForShow(UUID showId) {
        if (!showRepository.existsById(showId)) {
            throw new IllegalStateException("Show does not exist");
        }
        List<SeatAvailability> seats = seatAvailabilityRepository.findByShowId(showId);

        Show show = showRepository.findById(showId).orElseThrow(() -> new IllegalStateException("Show does not exist"));

        return Map.of(
                "success", true,
                "message", "Seats retrieved successfully",
                "showtimeMultiplier", show.getShowtimeMultiplier(),
                "seats", seats.stream().map(SeatAvailabilityMapper::mapToSeatAvailabilityDto).toList()
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
        boolean isPenalized = userPenaltyRepository
                .existsByUserIdAndShowIdAndPenaltyExpiryAfterAndFailedLockCountGreaterThanEqual(
                        request.getUserId(), showId, OffsetDateTime.now(), PENALTY_BLOCK_THRESHOLD);

        if (isPenalized) {
            throw new IllegalStateException("You recently let a reservation expire. Please wait 15 minutes before trying to book seats for this show again.");
        }
        
        List<SeatAvailability> previousLocks = seatAvailabilityRepository
                .findByShowIdAndLockedByIdAndSeatStatus(showId, user.getId(), SeatStatus.LOCKED);
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
                .findByShowIdAndSeatIdInAndSeatStatus(showId, request.getSeatIds(), SeatStatus.AVAILABLE);

        if (targetSeats.size() != request.getSeatIds().size()) {
            throw new IllegalStateException("One or more seats do not exist for this show");
        }
        OffsetDateTime now = OffsetDateTime.now();
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
                "seats", lockedSeats.stream().map(SeatAvailabilityMapper::mapToSeatAvailabilityDto).toList()
        );
    }

    /**
     * Transitions all seats linked to a confirmed booking from LOCKED → BOOKED.
     * Called by BookingService.confirmPayment() after signature verification passes.
     */
    @Transactional
    public void markSeatsAsBooked(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found: " + bookingId));

        List<SeatAvailability> seats = booking.getItems().stream()
                .map(BookingItem::getSeatAvailability)
                .toList();

        seats.forEach(seat -> {
            seat.setSeatStatus(SeatStatus.BOOKED);
            seat.setLockedBy(null);
            seat.setLockedAt(null);
            seat.setLockExpiry(null);
        });

        seatAvailabilityRepository.saveAll(seats);
    }

}
