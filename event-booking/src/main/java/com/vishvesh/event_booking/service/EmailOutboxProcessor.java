package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.config.RabbitMQConfig;
import com.vishvesh.event_booking.entity.EmailOutboxEvent;
import com.vishvesh.event_booking.repository.EmailOutboxEventRepository;
import com.vishvesh.event_booking.utils.enums.OutboxStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxProcessor {

    private final EmailOutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

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
                Message message = MessageBuilder
                        .withBody(task.getId().toString().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                        .build();

                rabbitTemplate.send(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, message);
                task.setStatus(OutboxStatus.COMPLETED);

            } catch (Exception e) {
                log.error("Broker connection failed. Halting batch processing. Error: {}", e.getMessage());
                break;
            }
        }
    }
}