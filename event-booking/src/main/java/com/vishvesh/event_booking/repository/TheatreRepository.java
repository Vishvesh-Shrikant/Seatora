package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, UUID> {
    List<Theatre> findByIsActiveTrue();
    List<Theatre> findByCityIgnoreCaseAndIsActiveTrue(String city);
}
