package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.SeatAvailability;
import com.vishvesh.event_booking.entity.UserBookingPenalty;

import com.vishvesh.event_booking.repository.SeatAvailabilityRepository;
import com.vishvesh.event_booking.repository.UserPenaltyRepository;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockCleanupService {

    private final SeatAvailabilityRepository seatAvailabilityRepository;
    private final UserPenaltyRepository penaltyRepository;
    private final BookingService bookingService;

    private static final int PENALTY_MINUTES = 15;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void unlockExpiredSeats() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SeatAvailability> expiredSeats = seatAvailabilityRepository
                .findBySeatStatusAndLockExpiryBefore(SeatStatus.LOCKED, now);

        if (!expiredSeats.isEmpty()) {

            Set<String> processedCarts = new HashSet<>();
            expiredSeats.forEach(seat -> {
                if (seat.getLockedBy() != null) {
                    String cartKey = seat.getLockedBy().getId() + ":" + seat.getShow().getId();

                    if (processedCarts.add(cartKey)) {
                        recordOrIncrementPenalty(seat.getLockedBy().getId(), seat.getShow().getId());
                    }
                }
            });

            // Step 2: Now release all the seats.
            expiredSeats.forEach(seat -> {
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seat.setLockedBy(null);
                seat.setLockedAt(null);
                seat.setLockExpiry(null);
            });

            seatAvailabilityRepository.saveAll(expiredSeats);
            bookingService.failExpiredPendingBookings();
        }
    }

    public void recordOrIncrementPenalty(UUID userId, UUID showId) {
        penaltyRepository.findByUserIdAndShowId(userId, showId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setFailedLockCount(existing.getFailedLockCount() + 1);
                            existing.setPenaltyExpiry(OffsetDateTime.now().plusMinutes(PENALTY_MINUTES));
                            penaltyRepository.save(existing);
                            log.warn("User {} has now abandoned seats {} time(s) for show {}",
                                    userId, existing.getFailedLockCount(), showId);
                        },
                        () -> {
                            UserBookingPenalty penalty = UserBookingPenalty.builder()
                                    .userId(userId)
                                    .showId(showId)
                                    .failedLockCount(1)
                                    .penaltyExpiry(OffsetDateTime.now().plusMinutes(PENALTY_MINUTES))
                                    .build();
                            penaltyRepository.save(penalty);
                            log.info("First abandonment recorded for user {} on show {}", userId, showId);
                        }
                );
    }
}
