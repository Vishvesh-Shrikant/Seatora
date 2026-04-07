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

}
