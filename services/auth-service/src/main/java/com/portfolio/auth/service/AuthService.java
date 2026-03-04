package com.portfolio.auth.service;

import com.portfolio.auth.dto.AuthResponse;
import com.portfolio.auth.dto.LoginRequest;
import com.portfolio.auth.dto.RegisterRequest;
import com.portfolio.auth.entity.User;
import com.portfolio.auth.repository.UserRepository;
import com.portfolio.common.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already in use: " + req.email());
        }

        User user = User.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .build();

        user = userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return buildResponse(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse buildResponse(User user) {
        // Embed user metadata as extra claims so the gateway can inject identity headers
        // without an additional DB round-trip.
        Map<String, Object> extraClaims = Map.of(
                "userId", user.getId(),
                "fullName", user.getFirstName() + " " + user.getLastName()
        );
        String token = jwtTokenProvider.generateToken(user.getEmail(), extraClaims);

        return new AuthResponse(token, user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName());
    }
}
