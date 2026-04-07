package com.vishvesh.event_booking.dto.seatavailability;

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
public class SeatLockRequestDto {
    List<UUID> seatIds;
    UUID userId;        
}
