package com.vishvesh.event_booking.utils.dto.theatre;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheatreRequestDto {
    private String Name;
    private String city;
    private String address;;
}
