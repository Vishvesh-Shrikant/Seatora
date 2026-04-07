package com.vishvesh.event_booking.dto.show;

import com.vishvesh.event_booking.utils.enums.ShowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShowRequestDto {
    private UUID movieId;
    private UUID screenId;
    private OffsetDateTime showDatetime;
    private ShowStatus showStatus;
    private BigDecimal price;
}
