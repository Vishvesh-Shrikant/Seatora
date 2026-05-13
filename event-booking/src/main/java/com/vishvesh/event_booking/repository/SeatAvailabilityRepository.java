package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.SeatAvailability;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SeatAvailabilityRepository extends JpaRepository<SeatAvailability, UUID> {
    List<SeatAvailability> findByShowId(UUID showId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SeatAvailability> findByShowIdAndSeatIdInAndSeatStatus(UUID showId, List<UUID> seatIds, SeatStatus status);
    List<SeatAvailability> findByShowIdAndLockedByIdAndSeatStatus(UUID showId, UUID userId, SeatStatus status);
    List<SeatAvailability> findBySeatStatusAndLockExpiryBefore(SeatStatus status, OffsetDateTime time);
}
