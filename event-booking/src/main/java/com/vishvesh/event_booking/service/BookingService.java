package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.dto.email.EmailPayloadDto;
import com.vishvesh.event_booking.entity.*;
import com.vishvesh.event_booking.repository.*;
import com.vishvesh.event_booking.utils.enums.BookingStatus;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import com.vishvesh.event_booking.utils.enums.PaymentStatus;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
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

    /**
     * Step 1 (Transactional): Validate the already-locked seats and persist the Booking record.
     * The Razorpay call is intentionally outside this transaction to avoid holding a DB
     * connection open during an external HTTP call.
     */
    @Transactional
    public Booking createPendingBooking(UUID userId, UUID showId, List<UUID> seatIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        // Seats must already be locked by the client via the /seats/lock endpoint.
        // We do NOT call lockSeatsForShow() here to avoid double-locking under load.
        List<SeatAvailability> lockedSeats = seatAvailabilityRepository
                .findByShowIdAndLockedByIdAndSeatStatus(showId, userId, SeatStatus.LOCKED);

        if (lockedSeats.isEmpty()) {
            throw new IllegalStateException("No locked seats found. Please lock seats before initiating checkout.");
        }

        List<BookingItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SeatAvailability seatAvail : lockedSeats) {
            if (!seatAvail.getShow().getId().equals(showId)) {
                throw new IllegalArgumentException("Seat " + seatAvail.getSeat().getSeatNo() + " does not belong to the selected show.");
            }
            BigDecimal price = seatAvail.getSeat().getBasePrice()
                    .multiply(seatAvail.getShow().getShowtimeMultiplier());
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

        log.info("Pending booking created: bookingId={}", booking.getId());
        return booking;
    }

    /**
     * Step 2 (non-transactional orchestrator): Calls Razorpay AFTER the DB transaction
     * has committed, then saves the Payment record in its own short transaction.
     * This ensures DB connections are never held open during an external HTTP call.
     */
    public Map<String, Object> initiateCheckout(UUID userId, UUID showId, List<UUID> seatIds) {
        // Phase 1: persist booking (transaction commits here)
        Booking booking = createPendingBooking(userId, showId, seatIds);

        // Phase 2: call Razorpay outside any transaction
        String razorpayOrderId = paymentGatewayService.createOrder(
                booking.getTotalAmount(), booking.getId().toString()
        );

        // Phase 3: persist payment in a fresh short transaction
        Payment payment = savePaymentRecord(booking.getId(), booking.getTotalAmount(), razorpayOrderId);

        log.info("Checkout initiated: bookingId={} razorpayOrderId={}", booking.getId(), razorpayOrderId);
        return Map.of(
                "success", true,
                "message", "Checkout initiated. Complete payment to confirm your booking.",
                "payment", payment
        );
    }

    @Transactional
    public Payment savePaymentRecord(UUID bookingId, BigDecimal amount, String razorpayOrderId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found: " + bookingId));
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .gatewayOrderId(razorpayOrderId)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        return paymentRepository.save(payment);
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

        } catch (JacksonException e) {
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

    /**
     * Pessimistic write lock on the Booking row prevents two concurrent scan
     * requests from both passing the isScanned check before either commits.
     * The SELECT ... FOR UPDATE ensures only one transaction proceeds at a time.
     */
    @Transactional
    public Map<String, Object> scanAndVerifyTicket(String qrData) {
        try {
            String[] parts = qrData.split("\\|");
            String bookingIdStr = parts[0].split(":")[1];
            UUID bookingId = UUID.fromString(bookingIdStr);

            // Pessimistic lock: SELECT ... FOR UPDATE
            Booking booking = bookingRepository.findByIdWithLock(bookingId)
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
                // Only release seats that are still LOCKED — BOOKED seats must not be touched.
                if (seat.getSeatStatus().equals(SeatStatus.LOCKED)) {
                    seat.setSeatStatus(SeatStatus.AVAILABLE);
                    seat.setLockedBy(null);
                    seat.setLockedAt(null);
                    seat.setLockExpiry(null);
                    seatsToRelease.add(seat);
                }
            }
        }
        bookingRepository.saveAll(expiredBookings);
        seatAvailabilityRepository.saveAll(seatsToRelease);

        log.info("Failed {} expired bookings and released {} seats.", expiredBookings.size(), seatsToRelease.size());
    }
}