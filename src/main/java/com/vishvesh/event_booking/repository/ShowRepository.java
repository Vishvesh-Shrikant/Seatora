package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.Show;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowRepository extends JpaRepository<Show, UUID> {
    List<Show> findByMovieId(UUID movieId);
    List<Show> findByMovieIdAndShowStatusAndShowDatetimeBetween(UUID movieId, ShowStatus status, OffsetDateTime startOfDay, OffsetDateTime endOfDay);
    List<Show> findByScreenId(UUID screenId); //admin only

    List<Show> findByShowStatus(ShowStatus status);  //admin only
    List<Show> findByShowDatetimeBetween(OffsetDateTime start, OffsetDateTime end);
    List<Show> findByScreenIdAndShowStatus(UUID screenId, ShowStatus status);
    List<Show> findByMovieIdAndShowStatus(UUID movieId, ShowStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Show s WHERE s.screen.id = :screenId " +
            "AND s.showStatus != 'CANCELLED' " +
            "AND s.showDatetime < :end " +
            "AND s.endDatetime > :start")
    List<Show> findOverlappingShows(UUID screenId, OffsetDateTime start, OffsetDateTime end);
}
