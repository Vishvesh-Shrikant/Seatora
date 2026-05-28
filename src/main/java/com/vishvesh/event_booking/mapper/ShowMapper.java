package com.vishvesh.event_booking.mapper;

import com.vishvesh.event_booking.dto.show.ShowResponseDto;
import com.vishvesh.event_booking.entity.Show;
import org.jspecify.annotations.NonNull;

public class ShowMapper {
    public static ShowResponseDto mapToShowResponseDto(@NonNull Show show) {
        return ShowResponseDto.builder()
                .showId(show.getId())
                .showStatus(show.getShowStatus())
                .showDatetime(show.getShowDatetime())
                .movie(MovieMapper.mapToMovieResponseDto(show.getMovie()))
                .screen(ScreenMapper.mapToScreenResponse(show.getScreen()))
                .theatre(TheatreMapper.mapToTheaterResponse(show.getScreen().getTheater()))
                .showtimeMultiplier(show.getShowtimeMultiplier())
                .build();
    }
}
