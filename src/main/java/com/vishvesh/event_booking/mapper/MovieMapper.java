package com.vishvesh.event_booking.mapper;

import com.vishvesh.event_booking.dto.movie.MovieResponseDto;
import com.vishvesh.event_booking.entity.Movie;
import org.jspecify.annotations.NonNull;

public class MovieMapper {
    public static MovieResponseDto mapToMovieResponseDto(@NonNull Movie movie) {
        return MovieResponseDto.builder()
                .movieId(movie.getId())
                .movieTitle(movie.getTitle())
                .genre(movie.getGenre())
                .movieDescription(movie.getDescription())
                .language(movie.getLanguage())
                .duration(movie.getDurationMinutes())
                .movieFormat(movie.getMovieFormat())
                .posterUrl(movie.getPosterUrl())
                .releaseDate(movie.getReleaseDate())
                .build();
    }
}
