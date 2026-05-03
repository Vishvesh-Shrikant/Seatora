package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {
    List<Screen> findByTheaterIdAndIsActiveTrue(UUID theaterId);
    List<Screen> findByTheaterId(UUID theaterId);
    boolean existsByTheaterIdAndScreenNoAndIsActiveTrue(UUID theaterId, String screenNo);
    boolean existsByTheaterIdAndIsActiveTrue(UUID theaterId);
}
