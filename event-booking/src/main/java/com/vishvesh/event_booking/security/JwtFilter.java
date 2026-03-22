package com.vishvesh.event_booking.security;

import com.vishvesh.event_booking.utils.CookieUtil;
import com.vishvesh.event_booking.utils.dto.authdto.JwtDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        cookieUtil.getJwtFromRequest(request)
                .flatMap(jwtService::parseToken)
                .ifPresent(this::setAuthentication);

        filterChain.doFilter(request, response);
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
