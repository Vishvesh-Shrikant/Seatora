package com.vishvesh.event_booking.entity;

import com.vishvesh.event_booking.utils.enums.AuthProvider;
import com.vishvesh.event_booking.utils.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "TEXT")
    @JdbcTypeCode(Types.VARCHAR)
    private UUID id;

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "TEXT")
    private String  email;

    @Column(name = "hashed_password", columnDefinition = "TEXT")
    private String hashedPassword;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role= Role.USER;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified=false;

    @Column(name = "google_id", columnDefinition = "TEXT")
    private String googleId;

    @Column(name = "verification_token", columnDefinition = "TEXT")
    private String verificationToken;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider = AuthProvider.CREDENTIAL;

    @Column(name = "verification_expiry")
    private OffsetDateTime verificationTokenExpiresAt;

    @Version
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


}
