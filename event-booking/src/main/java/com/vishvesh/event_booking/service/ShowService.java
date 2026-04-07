package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Movie;
import com.vishvesh.event_booking.entity.Screen;
import com.vishvesh.event_booking.entity.Show;
import com.vishvesh.event_booking.repository.MovieRepository;
import com.vishvesh.event_booking.repository.ScreenRepository;
import com.vishvesh.event_booking.repository.ShowRepository;
import com.vishvesh.event_booking.dto.show.ShowRequestDto;
import com.vishvesh.event_booking.dto.show.ShowResponseDto;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowService {
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;

    //for admin
    public Map<String, Object> getAllShowsByMovie(UUID movieId){
        if(movieRepository.findByIdAndIsActiveTrue(movieId).isEmpty()){
            throw new IllegalStateException("Movie is not active and is deleted");
        }
        List<Show> showsOfMovie= showRepository.findByMovieId(movieId);

        return Map.of("success", true, "message", "Shows retrieved successfully based on Movie", "shows", showsOfMovie.stream().map(this::mapToShowResponseDto).toList() );
    }

    //for user
    public Map<String, Object> getAvailableShowsOfMoviesForDate(UUID movieId, LocalDate targetDate){
        if(movieRepository.findByIdAndIsActiveTrue(movieId).isEmpty()){
            throw new IllegalStateException("Movie is not active and is deleted");
        }
        ZoneId theaterZone = ZoneId.systemDefault();
        OffsetDateTime startOfDay = targetDate.atStartOfDay(theaterZone).toOffsetDateTime();
        OffsetDateTime endOfDay = targetDate.atTime(LocalTime.MAX).atZone(theaterZone).toOffsetDateTime();

        List<Show> showsOfMovie= showRepository.findByMovieIdAndShowStatusAndShowDatetimeBetween(movieId, ShowStatus.SCHEDULED, startOfDay, endOfDay);

        return Map.of("success", true, "message", "Shows retrieved successfully based on Movie", "shows", showsOfMovie.stream().map(this::mapToShowResponseDto).toList() );
    }

    //for admin
    public Map<String, Object> getAllShowsByScreen(UUID screenId){
        if(screenRepository.findById(screenId).isEmpty()){
            throw new IllegalStateException("Screen is not active and is deleted");
        }
        List<Show> showsOfScreen= showRepository.findByScreenId(screenId);

        return Map.of("success", true, "message", "Shows retrieved successfully on basis of Screen", "shows", showsOfScreen.stream().map(this::mapToShowResponseDto).toList() );
    }

    //for admin
    public Map<String, Object> getAllShowsBetweenDates(LocalDate start, LocalDate end){

        ZoneId theaterZone = ZoneId.systemDefault();
        OffsetDateTime startDate = start.atStartOfDay(theaterZone).toOffsetDateTime();
        OffsetDateTime endDate = end.atTime(LocalTime.MAX).atZone(theaterZone).toOffsetDateTime();

        List<Show> showsBetweenDates = showRepository.findByShowDatetimeBetween(startDate, endDate);

        return Map.of("success", true, "message", "Shows retrieved successfully between given dates", "shows", showsBetweenDates.stream().map(this::mapToShowResponseDto).toList() );
    }

    public Map<String, Object> getAllShowsByShowStatus(String showStatus){

        List<Show> showsOfStatus= showRepository.findByShowStatus(ShowStatus.valueOf(showStatus));

        return Map.of("success", true, "message", "Shows retrieved successfully on basis of Show Status", "shows", showsOfStatus.stream().map(this::mapToShowResponseDto).toList() );

    }

    @Transactional
    public Map<String, Object> addNewShow(@NonNull ShowRequestDto showRequestDto){
        Movie movie=movieRepository.findById(showRequestDto.getMovieId()).orElseThrow(()-> new IllegalStateException("Movie is not active and is deleted"));

        Screen screen=screenRepository.findById(showRequestDto.getScreenId()).orElseThrow(()-> new IllegalStateException("Screen is not active and is deleted"));

        Show show = Show.builder()
                .showStatus(showRequestDto.getShowStatus())
                .movie(movie)
                .screen(screen)
                .basePrice(showRequestDto.getPrice())
                .showDatetime(showRequestDto.getShowDatetime())
                .build();

        Show savedShow = showRepository.save(show);
        return Map.of("success", true, "message", "New show added successfully", "shows", mapToShowResponseDto(savedShow));
    }

    @Transactional
    public Map<String, Object> updateShow(UUID showId, @NonNull ShowRequestDto showRequestDto){
        Show show = showRepository.findById(showId).orElseThrow(()-> new IllegalStateException("Show doesn't exist"));
        Movie movie=movieRepository.findById(showRequestDto.getMovieId()).orElseThrow(()-> new IllegalStateException("Movie is not active and is deleted"));

        Screen screen=screenRepository.findById(showRequestDto.getScreenId()).orElseThrow(()-> new IllegalStateException("Screen is not active and is deleted"));

        show.setShowStatus(showRequestDto.getShowStatus());
        show.setShowDatetime(showRequestDto.getShowDatetime());
        show.setMovie(movie);
        show.setScreen(screen);
        show.setBasePrice(showRequestDto.getPrice());

        Show updatedShow= showRepository.save(show);

        return Map.of("success", true, "message", "Show updated successfully", "shows", mapToShowResponseDto(updatedShow));

    }

    @Transactional
    public Map<String, Object> cancelShow(UUID showId){
        Show show = showRepository.findById(showId).orElseThrow(()-> new IllegalStateException("Show doesn't exist"));

        show.setShowStatus(ShowStatus.CANCELLED);
        Show cancelledShow = showRepository.save(show);

        return Map.of("success", true, "message", "Show cancelled Successfully", "shows", mapToShowResponseDto(cancelledShow));

    }


    private ShowResponseDto mapToShowResponseDto(Show show){
        return ShowResponseDto.builder()
                .showId(show.getId())
                .showStatus(show.getShowStatus())
                .showDatetime(show.getShowDatetime())
                .movieId(show.getMovie().getId())
                .screenId(show.getScreen().getId())
                .price(show.getBasePrice())
                .build();
    }
}
