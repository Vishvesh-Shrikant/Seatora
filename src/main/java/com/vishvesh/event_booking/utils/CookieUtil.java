package com.vishvesh.event_booking.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    public static final String COOKIE_NAME = "auth_token";
    private final long EXPIRY_TIME;

    public CookieUtil(@Value("${JWT_EXPIRY}") long expiry) {
        this .EXPIRY_TIME = expiry;
    }
    private String buildCookieHeader(String value, int maxAgeSeconds) {
        return COOKIE_NAME + "=" + value
                + "; Max-Age=" + maxAgeSeconds
                + "; Path=/"
                + "; HttpOnly"
                + "; Secure"
                + "; SameSite=None";
    }

    public void addJwtCookie(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", buildCookieHeader(token, (int)(EXPIRY_TIME*60*60)));
    }

    public void clearJwtCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookieHeader("", 0));
    }

    public Optional<String> getJwtFromRequest(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
