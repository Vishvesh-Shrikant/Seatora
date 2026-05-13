package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.dto.email.EmailPayloadDto;
import com.vishvesh.event_booking.dto.seatavailability.SeatLockRequestDto;
import com.vishvesh.event_booking.entity.*;
import com.vishvesh.event_booking.repository.*;
import com.vishvesh.event_booking.utils.enums.BookingStatus;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import com.vishvesh.event_booking.utils.enums.PaymentStatus;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
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
    private final EmailOutboxEventRepository emailOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> initiateCheckout(UUID userId, UUID showId, List<UUID> seatIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        // Enforce concurrency control: Lock seats immediately before querying prices
        SeatLockRequestDto   seatLockRequestDto = new SeatLockRequestDto();
        seatLockRequestDto.setUserId(user.getId());
        seatLockRequestDto.setSeatIds(seatIds);

        seatAvailabilityService.lockSeatsForShow(showId, seatLockRequestDto);
        List<SeatAvailability> lockedSeats = seatAvailabilityRepository.findByShowIdAndLockedByIdAndSeatStatus(showId, userId, SeatStatus.LOCKED);
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookingItem> items = new ArrayList<>();

        for (SeatAvailability seatAvail : lockedSeats) {
            // Verify the seat belongs to the requested show
            if (!seatAvail.getShow().getId().equals(showId)) {
                throw new IllegalArgumentException("Seat " + seatAvail.getSeat().getSeatNo() + " does not belong to the selected show.");
            }

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
    public void confirmPaymentAndQueueEmail(String orderId, String paymentId, String signature) {
        // 1. Cryptographic Verification
        boolean isValid = paymentGatewayService.verifySignature(orderId, paymentId, signature);
        if (!isValid) {
            throw new SecurityException("Invalid payment signature detected.");
        }

        // 2. Fetch Payment and update status
        Payment payment = paymentRepository.findByGatewayOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found for order: " + orderId));

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setGatewayPaymentId(paymentId);
        paymentRepository.save(payment);

        // 3. Update Booking Status
        Booking booking = payment.getBooking();
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        seatAvailabilityService.markSeatsAsBooked(booking.getId());
        bookingRepository.save(booking);

        // 4. Create Queue Payload
        EmailPayloadDto payload = new EmailPayloadDto(booking.getId(), booking.getUser().getId());

        // 5. Save to Outbox (Transactional Dual-Write)
        try {
            EmailOutboxEvent outboxEvent = EmailOutboxEvent.builder()
                    .emailType("BOOKING_CONFIRMATION")
                    .bookingId(booking.getId().toString())
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxStatus.PENDING)
                    .build();

            emailOutboxRepository.save(outboxEvent);
            log.info("Booking {} confirmed. Email task written to outbox.", booking.getId());

        } catch (JsonParseException e) { // Fixed exception type
            log.error("Critical failure: Could not serialize outbox payload for booking {}", booking.getId(), e);
            throw new RuntimeException("Failed to queue email task", e);
        }
    }

    @Transactional
    public Map<String, Object> generateQrCode(UUID bookingId, UUID requestingUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found"));

        // IDOR Protection: Validate ownership
        if (!booking.getUser().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("You do not have permission to access this ticket.");
        }

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is not confirmed yet");
        }

        String ticketData = String.format("BOOKING:%s|USER:%s",
                booking.getId().toString(),
                booking.getUser().getId().toString());

        byte[] qrImage = qrCodeService.generateTicketQrCode(ticketData);

        return Map.of("success", true, "message", "QR code generated successfully", "qrcode", qrImage);
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

        if (expiredBookings.isEmpty()) return;

        List<SeatAvailability> seatsToRelease = new ArrayList<>();

        for (Booking booking : expiredBookings) {
            booking.setBookingStatus(BookingStatus.FAILED);

            // Critical fix: Release locked seats to prevent inventory leaks
            for (BookingItem item : booking.getItems()) {
                SeatAvailability seat = item.getSeatAvailability();
                if (seat.getSeatStatus().equals(SeatStatus.AVAILABLE)) {
                    seat.setSeatStatus(SeatStatus.AVAILABLE);
                    seat.setLockedBy(null);
                    seatsToRelease.add(seat);
                }
            }
        }
        bookingRepository.saveAll(expiredBookings);
        seatAvailabilityRepository.saveAll(seatsToRelease);

        log.info("Failed {} expired bookings and released {} seats.", expiredBookings.size(), seatsToRelease.size());
    }
}