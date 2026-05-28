package com.vishvesh.event_booking.dto.checkout;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CheckoutRequestDto {

    @NotNull(message = "showId is required")
    private UUID showId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<UUID> seatIds;
}
