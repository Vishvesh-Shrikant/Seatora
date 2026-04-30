package com.vishvesh.event_booking.security;

import com.vishvesh.event_booking.utils.CookieUtil;
import com.vishvesh.event_booking.dto.authdto.JwtDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtFilter(
            JwtService jwtService,
            CookieUtil cookieUtil,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            cookieUtil.getJwtFromRequest(request)
                    .flatMap(jwtService::parseToken)
                    .ifPresent(this::setAuthentication);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("JWT Authentication failed: {}", ex.getMessage());
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private void setAuthentication(JwtDto data) {
        var auth = new UsernamePasswordAuthenticationToken(
                data,
                null,
                List.of(new SimpleGrantedAuthority(data.getRole().toString()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}