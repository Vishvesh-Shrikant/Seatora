package com.vishvesh.event_booking.dto.seatavailability;

import com.vishvesh.event_booking.dto.seat.SeatResponseDto;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatAvailabilityResponseDto {
    UUID id;
    UUID showId;
    SeatResponseDto seat;
    UUID lockedByUserId;
    SeatStatus seatStatus;

}
