package com.vishvesh.event_booking.dto.seat;


import com.vishvesh.event_booking.utils.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SeatResponseDto {
    private UUID seatId;
    private UUID screenId;
    private String seatNo;
    private SeatType seatType;
}
