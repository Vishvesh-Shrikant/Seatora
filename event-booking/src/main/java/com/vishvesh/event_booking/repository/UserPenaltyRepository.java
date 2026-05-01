package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.UserBookingPenalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserPenaltyRepository extends JpaRepository<UserBookingPenalty, UUID> {

    // Used by SeatAvailabilityService to block users who have exceeded the failure threshold
    boolean existsByUserIdAndShowIdAndPenaltyExpiryAfterAndFailedLockCountGreaterThanEqual(
            UUID userId, UUID showId, OffsetDateTime now, int minCount);

    // Used by SeatLockCleanupService to find an existing record and increment the count
    Optional<UserBookingPenalty> findByUserIdAndShowId(UUID userId, UUID showId);
}
