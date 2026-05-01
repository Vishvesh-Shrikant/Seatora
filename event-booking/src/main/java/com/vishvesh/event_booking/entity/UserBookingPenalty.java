package com.vishvesh.event_booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_booking_penalties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBookingPenalty {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "penalty_expiry", nullable = false)
    private OffsetDateTime penaltyExpiry;

    // Tracks how many times the user has abandoned a lock for this show.
    // A hard block is only applied once this exceeds the threshold (default: 3).
    @Builder.Default
    @Column(name = "failed_lock_count", nullable = false)
    private int failedLockCount = 1;
}
