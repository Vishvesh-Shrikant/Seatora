package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.SeatAvailability;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SeatAvailabilityRepository extends JpaRepository<SeatAvailability, UUID> {
    List<SeatAvailability> findByShowId(UUID showId);
    List<SeatAvailability> findByShowIdAndSeatIdIn(UUID showId, List<UUID> seatIds);
    List<SeatAvailability> findByShowIdAndLockedByIdAndSeatStatus(UUID showId, UUID userId, SeatStatus status);
    List<SeatAvailability> findBySeatStatusAndLockExpiryBefore(SeatStatus status, OffsetDateTime time);
}
