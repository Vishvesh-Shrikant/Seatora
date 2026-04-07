package com.vishvesh.event_booking.entity;

import com.vishvesh.event_booking.utils.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seats_availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "TEXT")
    @JdbcTypeCode(Types.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false)
    private SeatStatus seatStatus = SeatStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by_user_id")
    private User lockedBy;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @Column(name = "lock_expiry")
    private OffsetDateTime lockExpiry;



    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}