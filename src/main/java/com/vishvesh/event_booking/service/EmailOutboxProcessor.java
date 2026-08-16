package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.EmailOutboxEvent;
import com.vishvesh.event_booking.repository.EmailOutboxEventRepository;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxProcessor {

    private final EmailOutboxEventRepository outboxRepository;
    private final EmailTaskExecutor emailTaskExecutor;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processOutbox() {
        List<EmailOutboxEvent> pendingTasks = outboxRepository
                .findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingTasks.isEmpty()) {
            return;
        }
        for (EmailOutboxEvent task : pendingTasks) {
            try {
                task.setStatus(OutboxStatus.PROCESSING);
                task.setRetryCount(task.getRetryCount() + 1);
                outboxRepository.save(task);
                
                emailTaskExecutor.processSingleEmailWithRetry(task);
            } catch (Exception e) {
                log.error("Error initiating email task execution: {}", e.getMessage());
            }
        }
    }
}