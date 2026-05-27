package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.service.AuthService;
import com.vishvesh.event_booking.utils.CookieUtil;
import com.vishvesh.event_booking.dto.authdto.AuthResponseDto;
import com.vishvesh.event_booking.dto.authdto.LoginDto;
import com.vishvesh.event_booking.dto.authdto.SignupDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupDto request) {

        System.out.println(request);
        Map <String, Object> response =  authService.signup(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid@RequestBody LoginDto request, HttpServletResponse response) {
        Map<String, Object> res = authService.login(request);

        if (Boolean.FALSE.equals(res.get("success")) || Boolean.FALSE.equals(res.get("verified"))) {
            return ResponseEntity.status(401).body(res);
        }
        AuthResponseDto user = (AuthResponseDto) res.get("user");
        String token = authService.mintToken(user);
        cookieUtil.addJwtCookie(response, token);
        return ResponseEntity.status(200).body(res);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody @NonNull Map<String, String> request) {
        String token = request.get("token");
        return ResponseEntity.status(200).body(authService.verifyEmail(token));
    }

    @PostMapping("/resendEmail")
    public ResponseEntity<Map<String, Object>> resendEmail(@RequestBody @NonNull Map<String, String> request) {
        String userIdStr = request.get("userId");
        if (userIdStr == null || userIdStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "userId is required"));
        }
        java.util.UUID userId = java.util.UUID.fromString(userIdStr);
        return ResponseEntity.ok(authService.resendEmail(userId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        cookieUtil.clearJwtCookie(response);
        return ResponseEntity.status(200).body(Map.of("success", "true", "message", "Logged out successfully."));
    }
}
