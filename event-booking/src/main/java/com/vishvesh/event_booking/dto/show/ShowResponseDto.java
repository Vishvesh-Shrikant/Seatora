package com.vishvesh.event_booking.dto.show;

import com.vishvesh.event_booking.dto.movie.MovieResponseDto;
import com.vishvesh.event_booking.dto.screen.ScreenResponseDto;
import com.vishvesh.event_booking.dto.theatre.TheatreResponseDto;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowResponseDto {
    private UUID showId;
    private MovieResponseDto movie;
    private ScreenResponseDto screen;
    private TheatreResponseDto theatre;
    private OffsetDateTime showDatetime;
    private ShowStatus showStatus;
    private BigDecimal showtimeMultiplier;
}
