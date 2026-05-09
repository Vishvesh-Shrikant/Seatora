package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Booking;
import com.vishvesh.event_booking.repository.BookingRepository;
import com.vishvesh.event_booking.utils.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailRetryService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final QrCodeService qrCodeService;

    /**
     * Periodically checks for CONFIRMED bookings where the email hasn't been sent.
     * This handles cases where the server crashed or the mail server was down.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void retryFailedEmails() {
        List<Booking> pendingEmails = bookingRepository
                .findByBookingStatusAndIsConfirmationEmailSentFalse(BookingStatus.CONFIRMED);

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.info("Found {} bookings with pending confirmation emails. Retrying...", pendingEmails.size());

        for (Booking booking : pendingEmails) {
            try {
                // Re-generate QR data exactly as done in BookingService
                String ticketData = String.format("BOOKING:%s|USER:%s",
                        booking.getId().toString(),
                        booking.getUser().getId().toString());
                
                byte[] qrCodeImage = qrCodeService.generateTicketQrCode(ticketData);
                
                // Call the email service (it's already @Async, but calling it here is fine)
                emailService.sendBookingConfirmation(booking.getId(), qrCodeImage);
                
            } catch (Exception e) {
                log.error("Failed to retry email for bookingId={}: {}", booking.getId(), e.getMessage());
            }
        }
    }
}
