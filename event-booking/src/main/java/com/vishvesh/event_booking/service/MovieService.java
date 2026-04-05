package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Movie;
import com.vishvesh.event_booking.repository.MovieRepository;
import com.vishvesh.event_booking.utils.dto.movie.MovieRequestDto;
import com.vishvesh.event_booking.utils.dto.movie.MovieResponseDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public Map<String, Object> getAllMovies(String searchTitle)
    {
        List<Movie> movies = (searchTitle != null && !searchTitle.isBlank())
                ? movieRepository.findByTitleContainingIgnoreCaseAndIsActiveTrue(searchTitle)
                : movieRepository.findByIsActiveTrue();

        return Map.of("success", true, "message", "Retrieved movie", "movies", movies.stream().map(this::mapToMovieResponseDto).toList());
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

        return Map.of("success", true, "message", "Movie updated successfully", "movie", mapToMovieResponseDto(updatedMovie));
    }

    public Map<String, Object> removeMovie(UUID id){
        Movie movie= movieRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        movie.setIsActive(false);
        Movie deletedMovie= movieRepository.save(movie);

        return Map.of("success", true, "message", "Movie updated successfully", "movie", mapToMovieResponseDto(deletedMovie));
    }

    private MovieResponseDto mapToMovieResponseDto(@NonNull Movie movie){
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
