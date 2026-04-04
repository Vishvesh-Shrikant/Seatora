package com.vishvesh.event_booking.utils.dto.screen;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScreenRequestDto {
    @NotNull
    private UUID theatreId;
    @NotBlank
    private String screenNo;
    @NotNull(message = "Total seats cannot be null")
    @Min(value = 1, message = "Screen must have at least 1 seat")
    private Integer totalSeats;

}
