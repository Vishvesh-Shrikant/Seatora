package com.vishvesh.event_booking.dto.authdto;

import com.vishvesh.event_booking.utils.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private UUID id;
    private String email;
    private String name;
    private Role role;
    private boolean isVerified;
}
