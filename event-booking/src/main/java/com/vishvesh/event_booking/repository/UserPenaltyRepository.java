package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.UserBookingPenalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface UserPenaltyRepository extends JpaRepository<UserBookingPenalty, UUID> {
    boolean existsByUserIdAndShowIdAndPenaltyExpiryAfter(UUID userId, UUID showId, OffsetDateTime now);
}
