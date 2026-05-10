package com.vishvesh.event_booking.mapper;

import com.vishvesh.event_booking.dto.screen.ScreenResponseDto;
import com.vishvesh.event_booking.entity.Screen;
import org.jspecify.annotations.NonNull;

public class ScreenMapper {
    public static ScreenResponseDto mapToScreenResponse(@NonNull Screen screen) {
        return ScreenResponseDto.builder()
                .screenId(screen.getId())
                .screenNo(screen.getScreenNo())
                .theatreId(screen.getTheater().getId())
                .totalSeats(screen.getTotalSeats())
                .build();
    }
}
