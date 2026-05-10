package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.dto.seatavailability.SeatLockRequestDto;
import com.vishvesh.event_booking.entity.*;
import com.vishvesh.event_booking.repository.BookingRepository;
import com.vishvesh.event_booking.repository.PaymentRepository;
import com.vishvesh.event_booking.repository.SeatAvailabilityRepository;
import com.vishvesh.event_booking.repository.UserRepository;
import com.vishvesh.event_booking.utils.enums.BookingStatus;
import com.vishvesh.event_booking.utils.enums.PaymentStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SeatAvailabilityRepository seatAvailabilityRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final PaymentGatewayService paymentGatewayService;
    private final SeatAvailabilityService seatAvailabilityService;
    private final EmailService emailService;


    @Transactional
    public Map<String, Object> initiateCheckout(UUID userId, UUID showId, List<UUID> seatIds) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        SeatLockRequestDto lockRequest = new SeatLockRequestDto();
        lockRequest.setUserId(userId);
        lockRequest.setSeatIds(seatIds);

        List<SeatAvailability> lockedSeats = seatAvailabilityRepository
                .findByShowIdAndSeatIdIn(showId, seatIds);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookingItem> items = new ArrayList<>();

        for (SeatAvailability seatAvail : lockedSeats) {
            BigDecimal seatPrice = seatAvail.getSeat().getBasePrice();
            BigDecimal multiplier = seatAvail.getShow().getShowtimeMultiplier();

            BigDecimal price = seatPrice.multiply(multiplier);

            totalAmount = totalAmount.add(price);
            items.add(BookingItem.builder()
                    .seatAvailability(seatAvail)
                    .price(price)
                    .build());
        }

        Booking booking = Booking.builder()
                .user(user)
                .totalAmount(totalAmount)
                .bookingStatus(BookingStatus.PENDING)
                .build();
        
        items.forEach(item -> item.setBooking(booking));
        booking.setItems(items);

        bookingRepository.save(booking);

        String razorpayOrderId = paymentGatewayService.createOrder(
                totalAmount, booking.getId().toString()
        );

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(totalAmount)
                .gatewayOrderId(razorpayOrderId)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        log.info("Checkout initiated: bookingId={} razorpayOrderId={}", booking.getId(), razorpayOrderId);

        return Map.of(
                "success", true,
                "message", "Checkout initiated. Complete payment to confirm your booking.",
                "payment", payment
        );
    }
    @Transactional
    public Map<String, Object> confirmPayment(String orderId, String paymentId, String signature) {

        boolean isValid = paymentGatewayService.verifySignature(orderId, paymentId, signature);
        if (!isValid) {
            throw new IllegalStateException("Security violation: Payment signature is invalid.");
        }

        String idempotencyKey = orderId + "|" + paymentId;
        if (paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.warn("Duplicate verify call detected for idempotencyKey: {}", idempotencyKey);
            return Map.of("success", true, "message", "Payment already verified and booking confirmed.");
        }

        Payment payment = paymentRepository.findByGatewayOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("No payment record found for order: " + orderId));

        // 4. Second guard — protect against race conditions between webhook and client
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return Map.of("success", true, "message", "Payment already verified and booking confirmed.");
        }


        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setGatewayPaymentId(paymentId);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        seatAvailabilityService.markSeatsAsBooked(booking.getId());

        // Generate QR code and dispatch confirmation email asynchronously
        String ticketData = String.format("BOOKING:%s|USER:%s",
                booking.getId().toString(),
                booking.getUser().getId().toString());
        byte[] qrCodeImage = qrCodeService.generateTicketQrCode(ticketData);
        emailService.sendBookingConfirmation(booking.getId(), qrCodeImage);

        log.info("Payment confirmed: bookingId={} paymentId={}", booking.getId(), paymentId);

        return Map.of(
                "success", true,
                "message", "Payment verified. Tickets confirmed!",
                "bookingId", booking.getId().toString()
        );
    }

    @Transactional
    public Map<String, Object> generateQrCode(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found"));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is not confirmed yet");
        }
        String ticketData = String.format("BOOKING:%s|USER:%s",
                booking.getId().toString(),
                booking.getUser().getId().toString());

        byte[] qrImage = qrCodeService.generateTicketQrCode(ticketData);

        return Map.of("success", "true", "message", "QR code generated successfully", "qrcode", qrImage);
    }

    @Transactional
    public Map<String, Object> scanAndVerifyTicket(String qrData) {
        try {
            String[] parts = qrData.split("\\|");
            String bookingIdStr = parts[0].split(":")[1];
            UUID bookingId = UUID.fromString(bookingIdStr);

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalStateException("Invalid QR Code: Booking not found."));

            if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
                return Map.of(
                        "valid", false,
                        "message", "PAYMENT INCOMPLETE: This booking is " + booking.getBookingStatus()
                );
            }

            if (booking.isScanned()) {
                return Map.of(
                        "valid", false,
                        "message", "ALREADY SCANNED: This ticket was already used for entry!"
                );
            }
            booking.setScanned(true);
            Booking scannedBooking = bookingRepository.save(booking);

            return Map.of(
                    "valid", true,
                    "message", "ENTRY APPROVED",
                    "seats", scannedBooking.getItems().size(),
                    "userName", scannedBooking.getUser().getName()
            );

        } catch (Exception e) {
            return Map.of(
                    "valid", false,
                    "message", "INVALID FORMAT: This is not a valid event ticket."
            );
        }
    }

    @Transactional
    public void failExpiredPendingBookings() {
        OffsetDateTime tenMinutesAgo = OffsetDateTime.now().minusMinutes(10);

        List<Booking> expiredBookings = bookingRepository
                .findByBookingStatusAndBookedAtBefore(BookingStatus.PENDING, tenMinutesAgo);

        if (!expiredBookings.isEmpty()) {
            expiredBookings.forEach(booking -> booking.setBookingStatus(BookingStatus.FAILED));
            bookingRepository.saveAll(expiredBookings);
        }
    }
}
