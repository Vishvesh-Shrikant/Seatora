//package com.vishvesh.event_booking.utils;
//
//import com.vishvesh.event_booking.utils.dto.ApiErrorResponse;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.servlet.resource.NoResourceFoundException;
//import java.time.OffsetDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//@RestControllerAdvice
//@Slf4j
//public class GlobalExceptionHandler {
//
//    // 1. Validation Errors (e.g., empty email, weak password)
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach(error -> {
//            String field = ((FieldError) error).getField();
//            errors.put(field, error.getDefaultMessage());
//        });
//
//        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", "Please check the provided fields.", request.getRequestURI(), errors);
//    }
//
//    // 2. Malformed JSON (User sent broken JSON)
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
//        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed Request", "The request body is missing or unreadable.", request.getRequestURI(), null);
//    }
//
//    // 3. Bad Requests (Your custom validations)
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
//        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI(), null);
//    }
//
//    // 4. Conflicts (e.g., trying to verify an already verified user)
//    @ExceptionHandler(IllegalStateException.class)
//    public ResponseEntity<ApiErrorResponse> handleConflict(IllegalStateException ex, HttpServletRequest request) {
//        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI(), null);
//    }
//
//    // 5. Login Failures (Security fix implemented here)
//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
//        // We explicitly coded the message here to prevent username enumeration attacks.
//        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Invalid email or password.", request.getRequestURI(), null);
//    }
//
//    // 6. Access Denied (User is logged in, but not an Admin)
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
//        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied", "You do not have permission to access this resource.", request.getRequestURI(), null);
//    }
//
//    // 7. Endpoint Not Found (404)
//    @ExceptionHandler(NoResourceFoundException.class)
//    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
//        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", "The requested endpoint does not exist.", request.getRequestURI(), null);
//    }
//
//    // 8. The Catch-All (500 Internal Server Error)
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
//        // We log the ACTUAL exception here for developers to fix
//        log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);
//
//        // But we hide the actual error from the user to prevent leaking database/code details
//        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", "An unexpected error occurred on our end. Please try again later.", request.getRequestURI(), null);
//    }
//
//    // --- Helper Method to keep code DRY (Don't Repeat Yourself) ---
//    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String error, String message, String path, Map<String, String> fieldErrors) {
//        ApiErrorResponse response = new ApiErrorResponse(
//                OffsetDateTime.now(),
//                status.value(),
//                error,
//                message,
//                path,
//                fieldErrors
//        );
//        return ResponseEntity.status(status).body(response);
//    }
//}
package com.vishvesh.event_booking.utils;

import com.vishvesh.event_booking.utils.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Validation Errors (e.g., empty email, weak password)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", "Please check the provided fields.", request.getRequestURI(), errors);
    }

    // 2. Malformed JSON (User sent broken JSON)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed Request", "The request body is missing or unreadable.", request.getRequestURI(), null);
    }

    // 3. Bad Requests (Your custom validations)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI(), null);
    }

    // 4. Conflicts (e.g., trying to verify an already verified user)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(IllegalStateException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI(), null);
    }

    // 5. Login Failures (Security fix implemented here)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        // We explicitly coded the message here to prevent username enumeration attacks.
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Invalid email or password.", request.getRequestURI(), null);
    }

    // 6. Access Denied (User is logged in, but lacks permissions/roles)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied", "You do not have permission to access this resource.", request.getRequestURI(), null);
    }

    // 7. Endpoint Not Found (404)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", "The requested endpoint does not exist.", request.getRequestURI(), null);
    }

    // 8. Unauthorized (NEW: Missing or invalid JWT Token)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "Please provide a valid authentication token to access this resource.", request.getRequestURI(), null);
    }

    // 9. The Catch-All (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        // We log the ACTUAL exception here for developers to fix
        log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);

        // But we hide the actual error from the user to prevent leaking database/code details
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", "An unexpected error occurred on our end. Please try again later.", request.getRequestURI(), null);
    }

    // --- Helper Method to keep code DRY (Don't Repeat Yourself) ---
    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String error, String message, String path, Map<String, String> fieldErrors) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                error,
                message,
                path,
                fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}