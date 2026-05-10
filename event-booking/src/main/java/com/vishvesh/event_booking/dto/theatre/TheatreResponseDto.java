package com.vishvesh.event_booking.dto.theatre;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheatreResponseDto {
    @NotBlank
    private UUID theatreId;
    @NotBlank
    private String theatreName;
    @NotBlank
    private String theatreAddress;
    @NotBlank
    private String theatreCity;
}
