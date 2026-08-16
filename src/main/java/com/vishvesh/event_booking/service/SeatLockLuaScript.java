package com.vishvesh.event_booking.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class SeatLockLuaScript {

    @Bean
    public DefaultRedisScript<Long> seatLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
                "-- KEYS[1..N] = seat lock keys (e.g., 'seat:lock:{showId}:{seatId}')\n" +
                "-- ARGV[1] = userId (value to store)\n" +
                "-- ARGV[2] = ttl in seconds (600)\n" +
                "local userId = ARGV[1]\n" +
                "local ttl = tonumber(ARGV[2])\n" +
                "\n" +
                "-- Phase 1: Check all seats are free\n" +
                "for i = 1, #KEYS do\n" +
                "    if redis.call('EXISTS', KEYS[i]) == 1 then\n" +
                "        return 0  -- Seat already locked -> reject entire batch\n" +
                "    end\n" +
                "end\n" +
                "\n" +
                "-- Phase 2: Lock all seats atomically\n" +
                "for i = 1, #KEYS do\n" +
                "    redis.call('SET', KEYS[i], userId, 'EX', ttl)\n" +
                "end\n" +
                "\n" +
                "return 1  -- All seats locked successfully\n"
        );
        script.setResultType(Long.class);
        return script;
    }
}
