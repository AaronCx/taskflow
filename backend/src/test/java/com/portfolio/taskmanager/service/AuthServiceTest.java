package com.portfolio.taskmanager.service;

import com.portfolio.taskmanager.dto.request.LoginRequest;
import com.portfolio.taskmanager.dto.request.RegisterRequest;
import com.portfolio.taskmanager.dto.response.AuthResponse;
import com.portfolio.taskmanager.entity.RefreshToken;
import com.portfolio.taskmanager.entity.User;
import com.portfolio.taskmanager.exception.ConflictException;
import com.portfolio.taskmanager.repository.UserRepository;
import com.portfolio.taskmanager.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock JwtTokenProvider      jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;
    @Mock RefreshTokenService   refreshTokenService;

    @InjectMocks AuthService authService;

    private RegisterRequest registerRequest;
    private User            savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Jane", "Doe", "jane@test.com", "secret123");

        savedUser = User.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@test.com")
                .password("hashed")
                .build();
    }

    // ── register ──────────────────────────────────────────────────────

    @Test
    @DisplayName("register: happy path returns AuthResponse with token")
    void register_success() {
        when(userRepository.existsByEmail("jane@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");
        RefreshToken mockRefresh = RefreshToken.builder().token("refresh-token").user(savedUser).build();
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(mockRefresh);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.email()).isEqualTo("jane@test.com");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: throws ConflictException when email already exists")
    void register_emailAlreadyExists() {
        when(userRepository.existsByEmail("jane@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("jane@test.com");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: valid credentials return AuthResponse")
    void login_success() {
        LoginRequest loginRequest = new LoginRequest("jane@test.com", "secret123");

        when(userRepository.findByEmail("jane@test.com")).thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.generateToken(savedUser)).thenReturn("fresh-token");
        RefreshToken mockRefresh = RefreshToken.builder().token("refresh-token").user(savedUser).build();
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(mockRefresh);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("fresh-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: bad credentials propagate BadCredentialsException")
    void login_badCredentials() {
        LoginRequest loginRequest = new LoginRequest("jane@test.com", "wrong");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
