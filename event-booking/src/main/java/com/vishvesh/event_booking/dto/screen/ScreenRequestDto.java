package com.vishvesh.event_booking.dto.screen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
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
    private Integer totalSeats;
    private List<LayoutRowDto> rows;

}
