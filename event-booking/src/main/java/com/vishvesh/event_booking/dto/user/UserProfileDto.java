package com.vishvesh.event_booking.dto.user;

import com.vishvesh.event_booking.utils.enums.AuthProvider;
import com.vishvesh.event_booking.utils.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserProfileDto {
    private UUID userId;
    private String name;
    private String email;
    private Role role;
    private Boolean isVerified;
    private AuthProvider authProvider;
}