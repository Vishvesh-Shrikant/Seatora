package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Booking;
import com.vishvesh.event_booking.entity.EmailOutboxEvent;
import com.vishvesh.event_booking.repository.BookingRepository;
import com.vishvesh.event_booking.repository.EmailOutboxEventRepository;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTaskExecutor {

    private final EmailService emailService;
    private final QrCodeService qrCodeService;
    private final BookingRepository bookingRepository;
    private final EmailOutboxEventRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 10000)
    )
    public void processSingleEmailWithRetry(EmailOutboxEvent task) {
        log.info("Processing email task: {}", task.getId());
        UUID bookingId = UUID.fromString(task.getBookingId());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        String ticketData = String.format("BOOKING:%s|USER:%s",
                booking.getId().toString(),
                booking.getUser().getId().toString());

        byte[] qrCodeImage = qrCodeService.generateTicketQrCode(ticketData);

        emailService.sendBookingConfirmation(booking.getId(), qrCodeImage);

        task.setStatus(OutboxStatus.COMPLETED);
        outboxRepository.save(task);
    }

    @Recover
    public void recover(Exception e, EmailOutboxEvent task) {
        log.error("Exhausted all retries for email task {}. Pushing to DLQ.", task.getId());
        task.setStatus(OutboxStatus.FAILED);
        outboxRepository.save(task);

        redisTemplate.opsForList().leftPush("email:dlq", task.getId().toString());
    }
}
