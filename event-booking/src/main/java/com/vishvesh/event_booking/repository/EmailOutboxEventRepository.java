package com.vishvesh.event_booking.repository;

import com.vishvesh.event_booking.entity.EmailOutboxEvent;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailOutboxEventRepository extends JpaRepository<EmailOutboxEvent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")
    })
    List<EmailOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus outboxStatus);
}
