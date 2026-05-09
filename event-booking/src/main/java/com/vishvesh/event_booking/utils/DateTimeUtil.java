package com.vishvesh.event_booking.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class DateTimeUtil {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    public static OffsetDateTime getStartOfDay(LocalDate date) {
        return date.atStartOfDay(SYSTEM_ZONE).toOffsetDateTime();
    }

    public static OffsetDateTime getEndOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX).atZone(SYSTEM_ZONE).toOffsetDateTime();
    }
}
