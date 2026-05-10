package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Movie;
import com.vishvesh.event_booking.entity.Show;
import com.vishvesh.event_booking.repository.MovieRepository;
import com.vishvesh.event_booking.repository.ShowRepository;
import com.vishvesh.event_booking.dto.movie.MovieRequestDto;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import com.vishvesh.event_booking.mapper.MovieMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;

    public Map<String, Object> getAllMovies(String searchTitle)
    {
        List<Movie> movies = (searchTitle != null && !searchTitle.isBlank())
                ? movieRepository.findByTitleContainingIgnoreCaseAndIsActiveTrue(searchTitle)
                : movieRepository.findByIsActiveTrue();

        return Map.of("success", true, "message", "Retrieved movie", "movies", movies.stream().map(MovieMapper::mapToMovieResponseDto).toList());
    }

    public Map<String, Object> addMovie(@NonNull MovieRequestDto movieRequestDto){
        if(movieRepository.existsByTitleIgnoreCaseAndIsActiveTrue(movieRequestDto.getTitle()))
        {
            throw new IllegalStateException("Movie with title: " + movieRequestDto.getTitle() + " already exists.");
        }

        Movie movie = Movie.builder()
                .title(movieRequestDto.getTitle())
                .description(movieRequestDto.getDescription())
                .durationMinutes(movieRequestDto.getDurationMinutes())
                .language(movieRequestDto.getLanguage())
                .genre(movieRequestDto.getGenre())
                .releaseDate(movieRequestDto.getReleaseDate())
                .posterUrl(movieRequestDto.getPosterUrl())
                .movieFormat(movieRequestDto.getMovieFormat())
                .build();
        movieRepository.save(movie);
        return Map.of("success", true, "message", "Movie added successfully", "movie", movie);
    }

    public Map<String, Object> updateMovie(UUID id, @NonNull MovieRequestDto movieRequestDto){
        Movie movie = movieRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if(!movie.getTitle().equals(movieRequestDto.getTitle()) && movieRepository.existsByTitleIgnoreCaseAndIsActiveTrue(movieRequestDto.getTitle()))
        {
            throw new IllegalStateException("Movie with title: " + movieRequestDto.getTitle() + " already exists.");
        }

        movie.setTitle(movieRequestDto.getTitle());
        movie.setDescription(movieRequestDto.getDescription());
        movie.setDurationMinutes(movieRequestDto.getDurationMinutes());
        movie.setLanguage(movieRequestDto.getLanguage());
        movie.setGenre(movieRequestDto.getGenre());
        movie.setReleaseDate(movieRequestDto.getReleaseDate());
        movie.setPosterUrl(movieRequestDto.getPosterUrl());
        movie.setMovieFormat(movieRequestDto.getMovieFormat());
        Movie updatedMovie = movieRepository.save(movie);

        return Map.of("success", true, "message", "Movie updated successfully", "movie", MovieMapper.mapToMovieResponseDto(updatedMovie));
    }

    public Map<String, Object> removeMovie(UUID id){
        Movie movie = movieRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // FIX 1.2: Cancel all SCHEDULED shows for this movie before deactivating it
        List<Show> scheduledShows = showRepository.findByMovieIdAndShowStatus(
                id, ShowStatus.SCHEDULED);
        scheduledShows.forEach(show -> show.setShowStatus(ShowStatus.CANCELLED));
        showRepository.saveAll(scheduledShows);
        log.info("Cancelled {} scheduled show(s) for movieId={}", scheduledShows.size(), id);

        movie.setIsActive(false);
        movieRepository.save(movie);

        return Map.of("success", true, "message", "Movie removed and all scheduled shows cancelled.");
    }

}
