package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
        List<Seat> findByScreenIdAndIsActiveTrue(UUID screenId);
        boolean existsBySeatIdAndSeatNoAndIsActiveTrue(UUID seatId, String seatNo);
        int countByScreenIdAndIsActiveTrue(UUID seatId);
}
