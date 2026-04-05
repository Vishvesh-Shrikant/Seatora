package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    List<Movie> findByIsActiveTrue();
    List<Movie> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title);
    boolean existsByTitleIgnoreCaseAndIsActiveTrue(String title);
    Optional<Movie> findByIdAndIsActiveTrue(UUID id);
}
