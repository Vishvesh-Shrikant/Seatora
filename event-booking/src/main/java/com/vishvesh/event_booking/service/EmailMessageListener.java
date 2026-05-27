package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.config.RabbitMQConfig;
import com.vishvesh.event_booking.dto.email.EmailPayloadDto;
import com.vishvesh.event_booking.entity.EmailOutboxEvent;
import com.vishvesh.event_booking.entity.Booking;
import com.vishvesh.event_booking.repository.BookingRepository;
import com.vishvesh.event_booking.repository.EmailOutboxEventRepository;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailMessageListener {

    private final EmailOutboxEventRepository outboxRepository;
    private final EmailService emailService;
    private final QrCodeService qrCodeService;
    private final ObjectMapper objectMapper;
    private final BookingRepository bookingRepository;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void receiveMessage(Message message) {
        String messagePayload = new String(message.getBody(), StandardCharsets.UTF_8);
        UUID outboxId;
        try {
            outboxId = UUID.fromString(messagePayload);
        } catch (IllegalArgumentException e) {
            log.error("Poison Pill detected: Invalid UUID format: {}", messagePayload);
            throw new AmqpRejectAndDontRequeueException("Unparseable payload", e);
        }

        // 2. Fetch the Outbox Event
        EmailOutboxEvent task = outboxRepository.findById(outboxId).orElse(null);
        if (task == null) {
            log.warn("Task not found in database: {}. Dropping message.", outboxId);
            return;
        }

        // 3. Idempotency Check
        if (OutboxStatus.COMPLETED.equals(task.getStatus())) {
            log.info("Task {} already COMPLETED. Ignoring.", outboxId);
            return;
        }

        // 4. Process Task
        try {
            UUID bookingId;
            UUID userId;

            // Safer JSON Parsing using DTO mapping
            try {
                EmailPayloadDto payloadDto = objectMapper.readValue(task.getPayload(), EmailPayloadDto.class);
                bookingId = payloadDto.getBookingId();
                userId = payloadDto.getUserId();

                if (bookingId == null || userId == null) throw new IllegalArgumentException("Missing required IDs in JSON");
            } catch (Exception ex) {
                // Fallback to fragile pipe separation for backward compatibility
                String[] parts = task.getPayload().split("\\|");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Payload format invalid: " + task.getPayload());
                }
                bookingId = UUID.fromString(parts[0]);
                userId = UUID.fromString(parts[1]);
            }

            // Cross-verify with entity's bookingId
            if (task.getBookingId() != null && !task.getBookingId().trim().isEmpty()) {
                bookingId = UUID.fromString(task.getBookingId());
            }

            // Fetch booking to get full details for the JSON QR payload
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            String ticketData;
            if (booking != null && !booking.getItems().isEmpty()) {
                ticketData = objectMapper.writeValueAsString(java.util.Map.of(
                        "bookingId", booking.getId().toString(),
                        "userId", booking.getUser().getId().toString(),
                        "movie", booking.getItems().get(0).getSeatAvailability().getShow().getMovie().getTitle(),
                        "theatre", booking.getItems().get(0).getSeatAvailability().getShow().getScreen().getTheater().getName(),
                        "screen", booking.getItems().get(0).getSeatAvailability().getShow().getScreen().getScreenNo(),
                        "seats", booking.getItems().stream().map(i -> i.getSeatAvailability().getSeat().getSeatNo()).reduce((a, b) -> a + ", " + b).orElse(""),
                        "time", booking.getItems().get(0).getSeatAvailability().getShow().getShowDatetime().toString()
                ));
            } else {
                ticketData = String.format("BOOKING:%s|USER:%s", bookingId, userId);
            }

            byte[] qrCodeImage = qrCodeService.generateTicketQrCode(ticketData);

            emailService.sendBookingConfirmation(bookingId, qrCodeImage);

            // 5. Mark as Completed
            task.setStatus(OutboxStatus.COMPLETED);
            outboxRepository.save(task);

        } catch (IllegalArgumentException e) {
            // Unrecoverable -> Mark failed and reject permanently
            log.error("Unrecoverable data error for task: {}", outboxId, e);
            task.setStatus(OutboxStatus.FAILED);
            outboxRepository.save(task);
            throw new AmqpRejectAndDontRequeueException("Invalid task data", e);

        } catch (Exception e) {
            // Transient error (SMTP) -> Log and throw to trigger Spring AMQP retry
            // DO NOT save task.setStatus(FAILED) here. Wait until DLQ.
            log.error("Transient failure processing email task: {}. Spring will retry.", outboxId, e);
            throw new RuntimeException("Transient email processing failure", e);
        }
    }

    /**
     * DLQ Listener: This executes ONLY if the main queue exhausts all 3 retries.
     * It safely marks the database row as FAILED without status thrashing.
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_DLQ)
    public void processDeadLetter(Message message) {
        String messagePayload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            UUID outboxId = UUID.fromString(messagePayload);
            outboxRepository.findById(outboxId).ifPresent(task -> {
                log.error("Task {} exhausted all retries. Marking FAILED in database.", outboxId);
                task.setStatus(OutboxStatus.FAILED);
                outboxRepository.save(task);
            });
        } catch (Exception e) {
            log.error("Failed to process DLQ message: {}", messagePayload, e);
        }
    }
}