package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.Booking;
import com.vishvesh.event_booking.utils.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUserIdOrderByBookedAtDesc(UUID userId);
    List<Booking> findByBookingStatus(BookingStatus status);
    List<Booking> findByBookingStatusAndBookedAtBefore(BookingStatus status, OffsetDateTime time);
    List<Booking> findByBookingStatusAndIsConfirmationEmailSentFalse(BookingStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") UUID id);
}
