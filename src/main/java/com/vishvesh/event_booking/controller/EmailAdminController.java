package com.vishvesh.event_booking.controller;

import com.vishvesh.event_booking.service.EmailAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/email")
@RequiredArgsConstructor
public class EmailAdminController {

    private final EmailAdminService emailAdminService;

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getPendingEmails() {
        return ResponseEntity.ok(emailAdminService.getPendingEmails());
    }

    @PostMapping("/retry/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> retryEmailManually(@PathVariable UUID id) {
        return ResponseEntity.ok(emailAdminService.retryEmailManually(id));
    }
}
