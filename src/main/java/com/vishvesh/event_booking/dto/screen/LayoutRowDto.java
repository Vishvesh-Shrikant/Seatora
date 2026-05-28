package com.vishvesh.event_booking.dto.screen;

import com.vishvesh.event_booking.utils.enums.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LayoutRowDto {
    @NotBlank
    private String name;
    @NotNull
    @Min(1)
    private Integer count;
    @NotNull
    private SeatType type;
    @NotNull
    private java.math.BigDecimal basePrice;
}
