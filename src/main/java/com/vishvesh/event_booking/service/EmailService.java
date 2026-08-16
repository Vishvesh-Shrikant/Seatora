package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.Booking;
import com.vishvesh.event_booking.entity.BookingItem;
import com.vishvesh.event_booking.repository.BookingRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final String baseurl;
    private final String fromEmail;
    private final BookingRepository bookingRepository;

    public EmailService(JavaMailSender mailSender, 
                        @Value("${BASE_URL}") String baseurl, 
                        @Value("${EMAIL_USERNAME}") String fromEmail,
                        BookingRepository bookingRepository) {
        this.mailSender = mailSender;
        this.baseurl = baseurl;
        this.fromEmail = fromEmail;
        this.bookingRepository = bookingRepository;
    }


    public void sendVerificationEmail(String toEmail, String token, long expiry) {
        String link = baseurl + "/auth/verify-email?token=" + token;
        SimpleMailMessage message = getMailMessage(toEmail, expiry, link);
        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send verification email to {}: {}", toEmail, ex.getMessage());
            throw new RuntimeException("Could not send verification email", ex);
        }
    }

    @Transactional
    public void sendBookingConfirmation(java.util.UUID bookingId, byte[] qrCodeImage) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

            if (booking.isConfirmationEmailSent()) {
                log.info("Booking confirmation email already sent for bookingId={}. Skipping.", bookingId);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("🎟️ Your Tickets are Confirmed! - Seatora");

            // Derive show info from the first booking item
            BookingItem firstItem = booking.getItems().get(0);
            String movieTitle = firstItem.getSeatAvailability().getShow().getMovie().getTitle();
            String theatreName = firstItem.getSeatAvailability().getShow().getScreen().getTheater().getName();
            String screenNo   = firstItem.getSeatAvailability().getShow().getScreen().getScreenNo();
            String showTime   = firstItem.getSeatAvailability().getShow().getShowDatetime()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

            String seatList = booking.getItems().stream()
                    .map(item -> item.getSeatAvailability().getSeat().getSeatNo())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("N/A");

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
                          <div style="background-color: #1a1a2e; color: white; padding: 24px; text-align: center;">
                            <h1 style="margin: 0; font-size: 24px;">🎬 Booking Confirmed!</h1>
                            <p style="margin: 8px 0 0; color: #a0a0c0;">Your tickets are ready. Enjoy the show!</p>
                          </div>
                          <div style="padding: 24px; background-color: #f9f9f9;">
                            <table style="width: 100%%; border-collapse: collapse;">
                              <tr><td style="padding: 8px 0; color: #666;">🎥 Movie</td>     <td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                              <tr><td style="padding: 8px 0; color: #666;">🏛️ Theatre</td>   <td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                              <tr><td style="padding: 8px 0; color: #666;">📺 Screen</td>    <td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                              <tr><td style="padding: 8px 0; color: #666;">⏰ Show Time</td> <td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                              <tr><td style="padding: 8px 0; color: #666;">💺 Seats</td>     <td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                              <tr><td style="padding: 8px 0; color: #666;">💰 Total Paid</td><td style="padding: 8px 0; font-weight: bold;">₹%s</td></tr>
                            </table>
                            <div style="text-align: center; padding: 24px; background: white;">
                              <p style="color: #555; margin-bottom: 12px;">Show this QR code at the gate for entry</p>
                              <img src="cid:qrcode" alt="Ticket QR Code" style="width: 200px; height: 200px; border: 2px solid #1a1a2e; border-radius: 8px;" />
                            </div>
                          </div>
                          <div style="background-color: #1a1a2e; color: #a0a0c0; text-align: center; padding: 16px; font-size: 12px;">
                            <p style="margin: 0;">Seatora — Your Event Booking Partner</p>
                          </div>
                    </div>
                    """.formatted(movieTitle, theatreName, screenNo, showTime, seatList, booking.getTotalAmount().toPlainString());

            helper.setText(html, true); // true = isHtml

            // Embed QR code as inline image with Content-ID "qrcode"
            ByteArrayResource qrResource = new ByteArrayResource(qrCodeImage) {
                @Override
                public String getFilename() {
                    return "qrcode.png";
                }
            };
            
            helper.addInline("qrcode", qrResource, "image/png");

            mailSender.send(message);
            log.info("Booking confirmation email sent to {} for bookingId={}", booking.getUser().getEmail(), booking.getId());

            // Update status in DB
            booking.setConfirmationEmailSent(true);
            bookingRepository.save(booking);

        } catch (Exception ex) {
            log.error("Failed to send booking confirmation email for bookingId={}, error={}", bookingId, ex.getMessage());
            throw new RuntimeException("Email delivery failed", ex);
        }
    }


    private @NonNull SimpleMailMessage getMailMessage(String toEmail, long expiry, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Verify your email - Seatora");
        message.setText("""
                Hi there,
 
                Please verify your email address by clicking the link below.
                This link expires in %d hour.
 
                %s
 
                If you did not create an account, you can safely ignore this email.
                """.formatted(expiry, link));
        return message;
    }
}

