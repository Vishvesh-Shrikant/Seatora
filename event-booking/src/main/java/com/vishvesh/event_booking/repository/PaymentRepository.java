package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Used during payment verification to locate the pending payment record
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    // Idempotency guard: prevents double-processing if verify is called twice
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
