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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockCleanupService {

    private final SeatAvailabilityRepository seatAvailabilityRepository;
    private final UserPenaltyRepository penaltyRepository; // <-- New repo

    private static final int PENALTY_MINUTES = 15;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void unlockExpiredSeats() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SeatAvailability> expiredSeats = seatAvailabilityRepository
                .findBySeatStatusAndLockExpiryBefore(SeatStatus.LOCKED, now);

        if (!expiredSeats.isEmpty()) {
            expiredSeats.forEach(seat -> {
                // 1. Give the user a penalty BEFORE we wipe their name from the seat
                if (seat.getLockedBy() != null) {
                    UserBookingPenalty penalty = UserBookingPenalty.builder()
                            .userId(seat.getLockedBy().getUserId())
                            .showId(seat.getShow().getId())
                            .penaltyExpiry(OffsetDateTime.now().plusMinutes(PENALTY_MINUTES))
                            .build();
                    penaltyRepository.save(penalty);
                }
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seat.setLockedBy(null);
                seat.setLockedAt(null);
                seat.setLockExpiry(null);
            });

            seatAvailabilityRepository.saveAll(expiredSeats);
        }
    }
}