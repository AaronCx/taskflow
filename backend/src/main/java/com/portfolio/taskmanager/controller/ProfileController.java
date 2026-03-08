package com.portfolio.taskmanager.controller;

import com.portfolio.taskmanager.dto.request.ChangePasswordRequest;
import com.portfolio.taskmanager.dto.request.UpdateProfileRequest;
import com.portfolio.taskmanager.dto.response.UserSummary;
import com.portfolio.taskmanager.entity.User;
import com.portfolio.taskmanager.exception.ConflictException;
import com.portfolio.taskmanager.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile management")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "Get the current user's profile")
    public ResponseEntity<UserSummary> getProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserSummary.from(currentUser));
    }

    @PutMapping
    @Operation(summary = "Update the current user's profile")
    public ResponseEntity<UserSummary> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal User currentUser) {

        // Check email uniqueness if changed
        if (!currentUser.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email '" + request.email() + "' is already taken.");
        }

        currentUser.setFirstName(request.firstName());
        currentUser.setLastName(request.lastName());
        currentUser.setEmail(request.email());

        User saved = userRepository.save(currentUser);
        return ResponseEntity.ok(UserSummary.from(saved));
    }

    @PutMapping("/password")
    @Operation(summary = "Change the current user's password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(currentUser);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
