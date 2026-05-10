package com.vishvesh.event_booking.mapper;

import com.vishvesh.event_booking.dto.seat.SeatResponseDto;
import com.vishvesh.event_booking.entity.Seat;
import org.jspecify.annotations.NonNull;

public class SeatMapper {
    public static SeatResponseDto mapToSeatResponseDto(@NonNull Seat seat) {
        return SeatResponseDto.builder()
                .seatId(seat.getId())
                .seatNo(seat.getSeatNo())
                .screenId(seat.getScreen().getId())
                .basePrice(seat.getBasePrice())
                .seatType(seat.getSeatType())
                .build();
    }
}
