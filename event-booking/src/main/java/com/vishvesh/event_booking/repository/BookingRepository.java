package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.Booking;
import com.vishvesh.event_booking.utils.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUserIdOrderByBookedAtDesc(UUID userId);
    List<Booking> findByBookingStatus(BookingStatus status);
    List<Booking> findByBookingStatusAndBookedAtBefore(BookingStatus status, OffsetDateTime time);
}
