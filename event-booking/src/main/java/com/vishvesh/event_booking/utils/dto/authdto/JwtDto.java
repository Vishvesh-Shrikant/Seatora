package com.vishvesh.event_booking.utils.dto.authdto;

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
public class JwtDto {
    private UUID userId;
    private String email;
    private Role role;
    private Boolean isVerified;
}
