package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.EmailOutboxEvent;
import com.vishvesh.event_booking.repository.EmailOutboxEventRepository;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailAdminService {

    private final EmailOutboxEventRepository outboxRepository;
    private final EmailTaskExecutor emailTaskExecutor;

    public Map<String, Object> getPendingEmails() {
        List<EmailOutboxEvent> manualRequired = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.MANUAL_REQUIRED);
        List<EmailOutboxEvent> failed = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.FAILED);
        
        return Map.of(
            "success", true,
            "manualRequiredCount", manualRequired.size(),
            "failedCount", failed.size(),
            "manualRequired", manualRequired,
            "failed", failed
        );
    }

    public Map<String, Object> retryEmailManually(UUID id) {
        EmailOutboxEvent task = outboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() != OutboxStatus.MANUAL_REQUIRED && task.getStatus() != OutboxStatus.FAILED) {
            throw new IllegalStateException("Task is not in a state that requires manual retry.");
        }

        task.setStatus(OutboxStatus.PROCESSING);
        task.setRetryCount(0); // Reset for manual retry
        outboxRepository.save(task);

        try {
            emailTaskExecutor.processSingleEmailWithRetry(task);
            return Map.of("success", true, "message", "Email sent successfully.");
        } catch (Exception e) {
            log.error("Manual retry failed for task {}", id, e);
            return Map.of("success", false, "message", "Manual retry failed. Error: " + e.getMessage());
        }
    }
}
