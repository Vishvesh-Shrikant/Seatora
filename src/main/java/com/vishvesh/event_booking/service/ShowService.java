package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Movie;
import com.vishvesh.event_booking.entity.Screen;
import com.vishvesh.event_booking.entity.Show;
import com.vishvesh.event_booking.mapper.ShowMapper;
import com.vishvesh.event_booking.repository.MovieRepository;
import com.vishvesh.event_booking.repository.ScreenRepository;
import com.vishvesh.event_booking.repository.SeatAvailabilityRepository;
import com.vishvesh.event_booking.repository.SeatRepository;
import com.vishvesh.event_booking.repository.ShowRepository;
import com.vishvesh.event_booking.dto.show.ShowRequestDto;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import com.vishvesh.event_booking.utils.DateTimeUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowService {
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final SeatAvailabilityRepository seatAvailabilityRepository;

    //for admin
    public Map<String, Object> getAllShowsByMovie(UUID movieId){
        if(movieRepository.findByIdAndIsActiveTrue(movieId).isEmpty()){
            throw new IllegalStateException("Movie is not active and is deleted");
        }
        List<Show> showsOfMovie= showRepository.findByMovieId(movieId);

        return Map.of("success", true, "message", "Shows retrieved successfully based on Movie", "shows", showsOfMovie.stream().map(ShowMapper::mapToShowResponseDto).toList() );
    }

    public Map<String, Object> getShowById(UUID showId) {
        Show show = showRepository.findById(showId).orElseThrow(() -> new IllegalStateException("Show doesn't exist"));
        return Map.of("success", true, "message", "Show retrieved successfully", "show", ShowMapper.mapToShowResponseDto(show));
    }


    //for user
    @Transactional
    public Map<String, Object> getAvailableShowsOfMoviesForDate(UUID movieId, LocalDate targetDate){
        if(movieRepository.findByIdAndIsActiveTrue(movieId).isEmpty()){
            throw new IllegalStateException("Movie is not active and is deleted");
        }
        OffsetDateTime startOfDay = DateTimeUtil.getStartOfDay(targetDate);
        OffsetDateTime endOfDay = DateTimeUtil.getEndOfDay(targetDate);

        List<Show> showsOfMovie= showRepository.findByMovieIdAndShowStatusAndShowDatetimeBetween(movieId, ShowStatus.SCHEDULED, startOfDay, endOfDay);

        return Map.of("success", true, "message", "Shows retrieved successfully based on Movie", "shows", showsOfMovie.stream().map(ShowMapper::mapToShowResponseDto).toList() );
    }

    //for admin
    public Map<String, Object> getAllShowsByScreen(UUID screenId){
        if(screenRepository.findById(screenId).isEmpty()){
            throw new IllegalStateException("Screen is not active and is deleted");
        }
        List<Show> showsOfScreen= showRepository.findByScreenId(screenId);

        return Map.of("success", true, "message", "Shows retrieved successfully on basis of Screen", "shows", showsOfScreen.stream().map(ShowMapper::mapToShowResponseDto).toList() );
    }

    //for admin
    public Map<String, Object> getAllShowsBetweenDates(LocalDate start, LocalDate end){

        OffsetDateTime startDate = DateTimeUtil.getStartOfDay(start);
        OffsetDateTime endDate = DateTimeUtil.getEndOfDay(end);

        List<Show> showsBetweenDates = showRepository.findByShowDatetimeBetween(startDate, endDate);

        return Map.of("success", true, "message", "Shows retrieved successfully between given dates", "shows", showsBetweenDates.stream().map(ShowMapper::mapToShowResponseDto).toList() );
    }

    public Map<String, Object> getAllShowsByShowStatus(String showStatus){

        List<Show> showsOfStatus= showRepository.findByShowStatus(ShowStatus.valueOf(showStatus));

        return Map.of("success", true, "message", "Shows retrieved successfully on basis of Show Status", "shows", showsOfStatus.stream().map(ShowMapper::mapToShowResponseDto).toList() );

    }

    @Transactional
    public Map<String, Object> addNewShow(@NonNull ShowRequestDto showRequestDto){
        Movie movie=movieRepository.findById(showRequestDto.getMovieId()).orElseThrow(()-> new IllegalStateException("Movie is not active and is deleted"));

        Screen screen=screenRepository.findById(showRequestDto.getScreenId()).orElseThrow(()-> new IllegalStateException("Screen is not active and is deleted"));

        // Calculate end time (movie duration + 30 mins buffer)
        OffsetDateTime showStart = showRequestDto.getShowDatetime();
        OffsetDateTime showEnd = showStart.plusMinutes(movie.getDurationMinutes() + 30);

        // Check for clashing shows
        List<Show> clashingShows = showRepository.findOverlappingShows(screen.getId(), showStart, showEnd);
        if (!clashingShows.isEmpty()) {
            throw new IllegalStateException("Show time clashes with an existing show in this screen.");
        }

        Show show = Show.builder()
                .showStatus(showRequestDto.getShowStatus())
                .movie(movie)
                .screen(screen)
                .showtimeMultiplier(showRequestDto.getShowtimeMultiplier())
                .showDatetime(showStart)
                .endDatetime(showEnd)
                .build();

        Show savedShow = showRepository.save(show);

        // NEW: Automatically populate SeatAvailability for all active seats in this screen
        List<com.vishvesh.event_booking.entity.Seat> screenSeats = seatRepository.findByScreenIdAndIsActiveTrue(screen.getId());
        List<com.vishvesh.event_booking.entity.SeatAvailability> availabilities = screenSeats.stream().map(seat ->
                com.vishvesh.event_booking.entity.SeatAvailability.builder()
                        .show(savedShow)
                        .seat(seat)
                        .seatStatus(com.vishvesh.event_booking.utils.enums.SeatStatus.AVAILABLE)
                        .build()
        ).toList();
        seatAvailabilityRepository.saveAll(availabilities);

        return Map.of("success", true, "message", "New show added successfully and seats initialized", "shows", ShowMapper.mapToShowResponseDto(savedShow));
    }

    @Transactional
    public Map<String, Object> updateShow(UUID showId, @NonNull ShowRequestDto showRequestDto){
        Show show = showRepository.findById(showId).orElseThrow(()-> new IllegalStateException("Show doesn't exist"));
        Movie movie=movieRepository.findById(showRequestDto.getMovieId()).orElseThrow(()-> new IllegalStateException("Movie is not active and is deleted"));

        Screen screen=screenRepository.findById(showRequestDto.getScreenId()).orElseThrow(()-> new IllegalStateException("Screen is not active and is deleted"));

        // Calculate end time
        OffsetDateTime showStart = showRequestDto.getShowDatetime();
        OffsetDateTime showEnd = showStart.plusMinutes(movie.getDurationMinutes() + 30);

        // Check for clashing shows (excluding the current show itself)
        List<Show> clashingShows = showRepository.findOverlappingShows(screen.getId(), showStart, showEnd)
                .stream().filter(s -> !s.getId().equals(showId)).toList();
        if (!clashingShows.isEmpty()) {
            throw new IllegalStateException("Show time clashes with an existing show in this screen.");
        }

        show.setShowStatus(showRequestDto.getShowStatus());
        show.setShowDatetime(showStart);
        show.setEndDatetime(showEnd);
        show.setMovie(movie);
        show.setScreen(screen);
        show.setShowtimeMultiplier(showRequestDto.getShowtimeMultiplier());
        Show updatedShow= showRepository.save(show);

        return Map.of("success", true, "message", "Show updated successfully", "shows", ShowMapper.mapToShowResponseDto(updatedShow));

    }

    @Transactional
    public Map<String, Object> cancelShow(UUID showId){
        Show show = showRepository.findById(showId).orElseThrow(()-> new IllegalStateException("Show doesn't exist"));

        show.setShowStatus(ShowStatus.CANCELLED);
        Show cancelledShow = showRepository.save(show);

        return Map.of("success", true, "message", "Show cancelled Successfully", "shows", ShowMapper.mapToShowResponseDto(cancelledShow));

    }


}
