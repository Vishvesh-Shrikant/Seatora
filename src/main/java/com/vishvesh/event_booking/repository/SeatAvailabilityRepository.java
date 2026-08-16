package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.SeatAvailability;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SeatAvailabilityRepository extends JpaRepository<SeatAvailability, UUID> {

    @Query("SELECT sa FROM SeatAvailability sa " +
           "JOIN FETCH sa.seat " +
           "JOIN FETCH sa.show " +
           "LEFT JOIN FETCH sa.lockedBy " +
           "WHERE sa.show.id = :showId")
    List<SeatAvailability> findByShowId(@Param("showId") UUID showId);

    List<SeatAvailability> findByShowIdAndSeatIdIn(UUID showId, List<UUID> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SeatAvailability> findByShowIdAndSeatIdInAndSeatStatus(UUID showId, List<UUID> seatIds, SeatStatus status);

    List<SeatAvailability> findByShowIdAndLockedByIdAndSeatStatus(UUID showId, UUID userId, SeatStatus status);

    List<SeatAvailability> findBySeatStatusAndLockExpiryBefore(SeatStatus status, OffsetDateTime time);
}

