package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.EmailOutboxEvent;
import com.vishvesh.event_booking.repository.EmailOutboxEventRepository;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDlqWorker {

    private final StringRedisTemplate redisTemplate;
    private final EmailOutboxEventRepository outboxRepository;

    private static final int MAX_DLQ_RETRIES = 4;
    private static final int MAX_ITEMS_PER_RUN = 50;

    /**
     * Polls the Redis DLQ list every 5 minutes.
     * For each item:
     *   - If retryCount >= 4 → escalate to MANUAL_REQUIRED (admin must handle)
     *   - Else → reset to PENDING so the main EmailOutboxProcessor picks it up again
     *
     * This avoids calling EmailTaskExecutor directly (which would create a
     * second retry path outside the main outbox cycle) and caps processing
     * at 50 items per run to prevent runaway loops.
     */
    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void processDlq() {
        int processed = 0;
        String taskIdStr = redisTemplate.opsForList().rightPop("email:dlq");

        while (taskIdStr != null && processed < MAX_ITEMS_PER_RUN) {
            processed++;
            log.info("[EMAIL-DLQ] Popped task {} from DLQ", taskIdStr);
            try {
                UUID taskId = UUID.fromString(taskIdStr);
                EmailOutboxEvent task = outboxRepository.findById(taskId).orElse(null);

                if (task == null || task.getStatus() == OutboxStatus.COMPLETED) {
                    log.info("[EMAIL-DLQ] Skipping stale/completed task {}", taskIdStr);
                    taskIdStr = redisTemplate.opsForList().rightPop("email:dlq");
                    continue;
                }

                if (task.getRetryCount() >= MAX_DLQ_RETRIES) {
                    log.error("[EMAIL-DLQ] Task {} escalated to MANUAL_REQUIRED after {} attempts",
                            taskId, task.getRetryCount());
                    task.setStatus(OutboxStatus.MANUAL_REQUIRED);
                    outboxRepository.save(task);
                } else {
                    log.warn("[EMAIL-DLQ] Task {} re-queued for retry attempt #{}", taskId, task.getRetryCount() + 1);
                    task.setStatus(OutboxStatus.PENDING); // Re-enters main processOutbox() cycle
                    outboxRepository.save(task);
                }
            } catch (Exception e) {
                log.error("[EMAIL-DLQ] Failed to process item: {}", taskIdStr, e);
                // Push back so we don't lose it
                redisTemplate.opsForList().leftPush("email:dlq", taskIdStr);
                break; // Stop processing on error to avoid tight re-push loops
            }
            taskIdStr = redisTemplate.opsForList().rightPop("email:dlq");
        }

        if (processed > 0) {
            log.info("[EMAIL-DLQ] Processed {} items from DLQ this run", processed);
        }
    }
}
