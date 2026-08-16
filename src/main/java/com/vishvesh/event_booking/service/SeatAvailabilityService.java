package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.dto.seatavailability.SeatLockRequestDto;
import com.vishvesh.event_booking.entity.*;
import com.vishvesh.event_booking.repository.*;
import com.vishvesh.event_booking.utils.enums.SeatStatus;
import com.vishvesh.event_booking.utils.enums.ShowStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.vishvesh.event_booking.mapper.SeatAvailabilityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatAvailabilityService {

    private final SeatAvailabilityRepository seatAvailabilityRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final UserPenaltyRepository userPenaltyRepository;
    private final BookingRepository bookingRepository;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> seatLockScript;
    private final CacheManager cacheManager;

    private static final int LOCK_DURATION_MINS = 10;
    private static final int MAX_SEATS_PER_REQUEST = 6;
    private static final int PENALTY_BLOCK_THRESHOLD = 3;
    
    private String getSeatLockKey(UUID showId, UUID seatId) {
        return "seat:lock:" + showId + ":" + seatId;
    }

    @Cacheable(value = "seatMap", key = "#showId")
    public Map<String, Object> getSeatsForShow(UUID showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new IllegalStateException("Show does not exist"));

        List<SeatAvailability> seats = seatAvailabilityRepository.findByShowId(showId);

        return Map.of(
                "success", true,
                "message", "Seats retrieved successfully",
                "showtimeMultiplier", show.getShowtimeMultiplier(),
                "seats", seats.stream().map(SeatAvailabilityMapper::mapToSeatAvailabilityDto).toList()
        );
    }

    @Transactional
    @CacheEvict(value = "seatMap", key = "#showId")
    public Map<String, Object> lockSeatsForShow(UUID showId, SeatLockRequestDto request) {
        if (request.getSeatIds().size() > MAX_SEATS_PER_REQUEST) {
            throw new IllegalStateException("You can only book up to " + MAX_SEATS_PER_REQUEST + " seats at a time.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new IllegalStateException("Show not found"));
        if (show.getShowStatus() != ShowStatus.SCHEDULED) {
            throw new IllegalStateException("Tickets cannot be booked for a " + show.getShowStatus() + " show.");
        }
        boolean isPenalized = userPenaltyRepository
                .existsByUserIdAndShowIdAndPenaltyExpiryAfterAndFailedLockCountGreaterThanEqual(
                        request.getUserId(), showId, OffsetDateTime.now(), PENALTY_BLOCK_THRESHOLD);

        if (isPenalized) {
            throw new IllegalStateException("You recently let a reservation expire. Please wait 15 minutes before trying to book seats for this show again.");
        }
        
        // Clear previous locks for this user in this show
        List<SeatAvailability> previousLocks = seatAvailabilityRepository
                .findByShowIdAndLockedByIdAndSeatStatus(showId, user.getId(), SeatStatus.LOCKED);
        if (!previousLocks.isEmpty()) {
            previousLocks.forEach(seat -> {
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seat.setLockedBy(null);
                seat.setLockedAt(null);
                seat.setLockExpiry(null);
                redisTemplate.delete(getSeatLockKey(showId, seat.getSeat().getId()));
            });
            seatAvailabilityRepository.saveAll(previousLocks);
        }

        // Prepare Redis keys for Phase 1
        List<String> keys = request.getSeatIds().stream()
                .map(seatId -> getSeatLockKey(showId, seatId))
                .collect(Collectors.toList());

        // Phase 1: Redis Lua Gate (Atomic check & set)
        Long scriptResult = redisTemplate.execute(
                seatLockScript,
                keys,
                user.getId().toString(),
                String.valueOf(LOCK_DURATION_MINS * 60)
        );

        if (scriptResult == null || scriptResult == 0L) {
            throw new IllegalStateException("One or more seats are already being held by another user.");
        }

        // Phase 2: DB Persistence (Transactional)
        List<SeatAvailability> targetSeats;
        try {
            targetSeats = seatAvailabilityRepository
                    .findByShowIdAndSeatIdIn(showId, request.getSeatIds());

            if (targetSeats.size() != request.getSeatIds().size()) {
                throw new IllegalStateException("One or more seats do not exist for this show");
            }
            
            for (SeatAvailability seat : targetSeats) {
                if (seat.getSeatStatus() != SeatStatus.AVAILABLE) {
                    throw new IllegalStateException("Seat is not available in database.");
                }
            }

            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime expiryTime = now.plusMinutes(LOCK_DURATION_MINS);

            targetSeats.forEach(seat -> {
                seat.setSeatStatus(SeatStatus.LOCKED);
                seat.setLockedBy(user);
                seat.setLockedAt(now);
                seat.setLockExpiry(expiryTime);
            });

            seatAvailabilityRepository.saveAll(targetSeats);
            
        } catch (Exception e) {
            // Rollback Phase 1 on Phase 2 failure
            log.error("DB persistence failed during seat locking. Rolling back Redis locks.", e);
            redisTemplate.delete(keys);
            throw e;
        }

        return Map.of(
                "success", true,
                "message", "Seats retrieved successfully",
                "seats", targetSeats.stream().map(SeatAvailabilityMapper::mapToSeatAvailabilityDto).toList()
        );
    }

    @Transactional
    public void markSeatsAsBooked(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found: " + bookingId));

        List<SeatAvailability> seats = booking.getItems().stream()
                .map(item -> item.getSeatAvailability())
                .toList();

        if (seats.isEmpty()) return;
        UUID showId = seats.get(0).getShow().getId();

        List<String> keysToDelete = new ArrayList<>();
        seats.forEach(seat -> {
            seat.setSeatStatus(SeatStatus.BOOKED);
            seat.setLockedBy(null);
            seat.setLockedAt(null);
            seat.setLockExpiry(null);
            keysToDelete.add(getSeatLockKey(showId, seat.getSeat().getId()));
        });

        seatAvailabilityRepository.saveAll(seats);
        redisTemplate.delete(keysToDelete);
        
        // Evict cache programmatically since the method doesn't take showId as an argument directly
        var cache = cacheManager.getCache("seatMap");
        if (cache != null) {
            cache.evict(showId);
        }
    }

}
