package com.vishvesh.event_booking.service;

import com.vishvesh.event_booking.entity.User;
import com.vishvesh.event_booking.repository.UserRepository;
import com.vishvesh.event_booking.security.JwtService;
import com.vishvesh.event_booking.dto.authdto.AuthResponseDto;
import com.vishvesh.event_booking.dto.authdto.JwtDto;
import com.vishvesh.event_booking.dto.authdto.LoginDto;
import com.vishvesh.event_booking.dto.authdto.SignupDto;
import com.vishvesh.event_booking.utils.enums.AuthProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService{

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Value("${EMAIL_TOKEN_EXPIRY:24}")
    private long verificationExpiry;

    @Transactional
    public Map<String, Object> signup(@NonNull SignupDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .hashedPassword(bCryptPasswordEncoder.encode(request.getPassword()    ))
                .authProvider(AuthProvider.CREDENTIAL)
                .isVerified(false)                         // must verify email first
                .verificationToken(verificationToken)
                .verificationTokenExpiresAt(OffsetDateTime.now().plusSeconds(verificationExpiry*3600))
                .build();

        user = userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationToken, verificationExpiry);

        log.info("New LOCAL user registered: {}", user.getEmail());

        return Map.of("success", true,
                "message" , "User signed-up and created successfully",
                "createdUser", buildAuthResponse(user));
    }

    @Transactional
    public Map<String, Object> verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification link"));

        if (user.getVerificationTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Verification link has expired. Please request a new one.");
        }

        user.setIsVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());

        return Map.of("success", true,
                "message", "Email verified successfully");
    }

    public Map<String, Object> login(@NonNull LoginDto request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getAuthProvider() != AuthProvider.CREDENTIAL) {
            throw new BadCredentialsException(
                    "This account uses Google Sign-In. Please log in with Google.");
        }

        if (!bCryptPasswordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.getIsVerified()) {
            throw new IllegalStateException("Please verify your email before logging in.");
        }
        return Map.of("success", true, "message" ,
                "User signed-up and created successfully",
                "user" , buildAuthResponse(user));
    }

    public String mintToken(@NonNull AuthResponseDto user) {
        JwtDto data = JwtDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .isVerified(user.isVerified())
                .build();
        return jwtService.generateToken(data);
    }

    public AuthResponseDto buildAuthResponse(@NonNull User user) {
        return AuthResponseDto.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .build();
    }

    public AuthResponseDto findByEmail(@NonNull String email) {
        User user= userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return buildAuthResponse(user);
    }
}
