package com.vishvesh.event_booking.entity;

import com.vishvesh.event_booking.utils.enums.MovieFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "TEXT")
    @JdbcTypeCode(Types.VARCHAR)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "genre")
    private String genre;

    @Column(name = "release_date")
    private OffsetDateTime releaseDate;

    @Column(name = "poster_url", columnDefinition = "TEXT")
    private String posterUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "movie_format", nullable = false)
    private MovieFormat movieFormat;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Version
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}