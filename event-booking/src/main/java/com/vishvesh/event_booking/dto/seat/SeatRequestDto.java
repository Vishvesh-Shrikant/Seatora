package com.vishvesh.event_booking.dto.seat;

import com.vishvesh.event_booking.utils.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequestDto {
    @NotNull
    private UUID screenId;
    @NotBlank
    private String seatNo;
    @NotNull
    private SeatType seatType;
    private BigDecimal basePrice;
}
