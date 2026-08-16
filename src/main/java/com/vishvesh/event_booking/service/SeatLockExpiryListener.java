package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.SeatAvailability;
import com.vishvesh.event_booking.repository.SeatAvailabilityRepository;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockExpiryListener implements MessageListener {

    private final RedisMessageListenerContainer listenerContainer;
    private final SeatAvailabilityRepository seatAvailabilityRepository;
    private final SeatLockCleanupService seatLockCleanupService;
    private final CacheManager cacheManager;

    @PostConstruct
    public void init() {
        listenerContainer.addMessageListener(this, new PatternTopic("__keyevent@*__:expired"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith("seat:lock:")) {
            try {
                String[] parts = expiredKey.split(":");
                if (parts.length == 4) {
                    UUID showId = UUID.fromString(parts[2]);
                    UUID seatId = UUID.fromString(parts[3]);

                    List<SeatAvailability> targetSeats = seatAvailabilityRepository
                            .findByShowIdAndSeatIdIn(showId, List.of(seatId));

                    if (!targetSeats.isEmpty()) {
                        SeatAvailability seat = targetSeats.get(0);
                        if (seat.getSeatStatus() == SeatStatus.LOCKED && seat.getLockedBy() != null) {
                            UUID userId = seat.getLockedBy().getId();
                            
                            // Update DB
                            seat.setSeatStatus(SeatStatus.AVAILABLE);
                            seat.setLockedBy(null);
                            seat.setLockedAt(null);
                            seat.setLockExpiry(null);
                            seatAvailabilityRepository.save(seat);

                            // Increment penalty for user
                            seatLockCleanupService.recordOrIncrementPenalty(userId, showId);
                            log.info("Seat {} for show {} auto-released via Redis expiry", seatId, showId);
                            
                            // Evict cache
                            var cache = cacheManager.getCache("seatMap");
                            if (cache != null) {
                                cache.evict(showId);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process expired seat lock key: " + expiredKey, e);
            }
        }
    }
}
