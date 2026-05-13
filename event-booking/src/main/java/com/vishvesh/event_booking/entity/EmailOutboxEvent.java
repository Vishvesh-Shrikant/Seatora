package com.vishvesh.event_booking.entity;

import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "email_outbox",
        // CRITICAL: Index required for fast polling
        indexes = {
                @Index(name = "idx_outbox_status", columnList = "outbox_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Required for the RabbitMQ listener routing logic
    @Column(nullable = false)
    @Builder.Default
    private String emailType = "BOOKING_CONFIRMATION";

    private String bookingId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "outbox_status", nullable = false)
    private OutboxStatus status;

    // Concurrency protection: Prevents two workers from claiming the same row
    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}