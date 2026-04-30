package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.dto.authdto.JwtDto;
import com.vishvesh.event_booking.dto.user.UserProfileDto;
import com.vishvesh.event_booking.entity.User;
import com.vishvesh.event_booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional()
    public Map<String, Object> getUserProfile() {
        Object principal = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        if (!(principal instanceof JwtDto jwtData)) {
            throw new IllegalStateException("Security context does not contain a valid user token.");
        }

        User currentUser = userRepository.findByEmail(jwtData.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User no longer exists in the system."));

        UserProfileDto fetchedUser = UserProfileDto.builder()
                .userId(currentUser.getId())
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .role(currentUser.getRole())
                .isVerified(currentUser.getIsVerified())
                .authProvider(currentUser.getAuthProvider())
                .build();
        return Map.of("success", true, "message", "user fetched successfully", "user", fetchedUser);
    }
}
