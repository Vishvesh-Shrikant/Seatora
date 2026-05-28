package com.vishvesh.event_booking.mapper;

import com.vishvesh.event_booking.dto.theatre.TheatreResponseDto;
import com.vishvesh.event_booking.entity.Theatre;
import org.jspecify.annotations.NonNull;

public class TheatreMapper {
    public static TheatreResponseDto mapToTheaterResponse(@NonNull Theatre theatre) {
        return TheatreResponseDto.builder()
                .theatreId(theatre.getId())
                .theatreName(theatre.getName())
                .theatreCity(theatre.getCity())
                .theatreAddress(theatre.getAddress())
                .build();
    }
}
