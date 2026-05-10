package com.vishvesh.event_booking.mapper;

import com.vishvesh.event_booking.dto.seatavailability.SeatAvailabilityResponseDto;
import com.vishvesh.event_booking.entity.SeatAvailability;
import org.jspecify.annotations.NonNull;

public class SeatAvailabilityMapper {
    public static SeatAvailabilityResponseDto mapToSeatAvailabilityDto(@NonNull SeatAvailability seat) {
        return SeatAvailabilityResponseDto.builder()
                .id(seat.getId())
                .showId(seat.getShow().getId())
                .seatStatus(seat.getSeatStatus())
                .seatId(seat.getSeat().getId())
                .lockedByUserId(seat.getLockedBy() != null ? seat.getLockedBy().getId() : null)
                .build();
    }
}
