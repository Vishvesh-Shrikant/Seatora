package com.vishvesh.event_booking.utils.dto.movie;

import com.vishvesh.event_booking.utils.enums.MovieFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequestDto {
    @NotBlank(message = "Movie title is required")
    private String title;

    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @NotBlank(message = "Language is required")
    private String language;

    private String genre;

    @NotNull(message = "Release date is required")
    private OffsetDateTime releaseDate;

    private String posterUrl;

    @NotNull(message = "Movie format is required")
    private MovieFormat movieFormat;

}
