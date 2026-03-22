package com.vishvesh.event_booking.service;


import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String baseurl;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender, @Value("${BASE_URL}") String baseurl , @Value("${EMAIL_USERNAME}") String fromEmail) {
        this.mailSender = mailSender;
        this.baseurl = baseurl;
        this.fromEmail = fromEmail;
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
