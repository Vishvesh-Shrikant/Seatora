package com.vishvesh.event_booking.utils.dto.movie;

import com.vishvesh.event_booking.utils.enums.MovieFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponseDto {

    private UUID movieId;
    private String movieTitle;
    private String movieDescription;
    private Integer duration;
    private String language;
    private String genre;
    private OffsetDateTime releaseDate;
    private String posterUrl;
    private MovieFormat movieFormat;

}
