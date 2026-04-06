package com.vishvesh.event_booking.dto.screen;

import com.vishvesh.event_booking.entity.Theatre;
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
public class ScreenResponseDto {
    private UUID screenId;
    private String screenNo;
    private UUID theatreId;
    private Integer totalSeats;
}
