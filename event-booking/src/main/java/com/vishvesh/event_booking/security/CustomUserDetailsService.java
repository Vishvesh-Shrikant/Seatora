package com.vishvesh.event_booking.security;

import com.vishvesh.event_booking.entity.User;
import com.vishvesh.event_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {

        // 1. Fetch YOUR custom User entity from the database
        User myUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. Convert it into a Spring Security "UserDetails" object so the Bouncer can read it
        return org.springframework.security.core.userdetails.User.builder()
                .username(myUser.getEmail())
                .password(myUser.getHashedPassword())
                .roles(myUser.getRole().toString())
                .build();
    }
}