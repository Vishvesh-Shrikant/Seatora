package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.dto.authdto.JwtDto;
import com.vishvesh.event_booking.dto.checkout.CheckoutRequestDto;
import com.vishvesh.event_booking.dto.checkout.PaymentVerificationRequestDto;
import com.vishvesh.event_booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> initiateCheckout(
            @AuthenticationPrincipal JwtDto currentUser,
            @Valid @RequestBody CheckoutRequestDto request) {

        return ResponseEntity.status(201).body(
                bookingService.initiateCheckout(
                        currentUser.getUserId(),
                        request.getShowId(),
                        request.getSeatIds()
                )
        );
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @AuthenticationPrincipal JwtDto currentUser,
            @Valid @RequestBody PaymentVerificationRequestDto request) {

        // Pass cryptographic parameters to the service layer for validation
        Map<String, Object> result = bookingService.confirmPaymentAndQueueEmail(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        return ResponseEntity.status(200).body(result);
    }

    @GetMapping("/generateQr/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> generateQrCode(
            @AuthenticationPrincipal JwtDto currentUser,
            @PathVariable UUID bookingId) {

        // Pass userId to prevent IDOR vulnerabilities
        return ResponseEntity.status(200).body(
                bookingService.generateQrCode(bookingId, currentUser.getUserId())
        );
    }

    @PostMapping("/scanQR")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> scanTicket(@RequestBody String request) {
        return ResponseEntity.status(200).body(bookingService.scanAndVerifyTicket(request));
    }

    /** Returns the current user's booking history ordered by date descending. */
    @GetMapping("/myBookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyBookings(
            @AuthenticationPrincipal JwtDto currentUser) {

        return ResponseEntity.status(200).body(
                bookingService.getMyBookings(currentUser.getUserId())
        );
    }

    /** Returns full detail for a single booking (IDOR-protected). */
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getBookingDetail(
            @AuthenticationPrincipal JwtDto currentUser,
            @PathVariable UUID bookingId) {

        return ResponseEntity.status(200).body(
                bookingService.getBookingDetail(bookingId, currentUser.getUserId())
        );
    }
}