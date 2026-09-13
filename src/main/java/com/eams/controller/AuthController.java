package com.eams.controller;

import com.eams.dto.request.RegisterRequest;
import com.eams.dto.request.LoginRequest;
import com.eams.dto.response.LoginResponse;
import com.eams.dto.response.MeResponse;
import com.eams.entity.User;
import com.eams.repository.UserRepository;
import com.eams.service.AuthService;
import com.eams.service.EmailVerificationService;
import com.eams.dto.request.ChangePasswordRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;   // NEW

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        "Registration successful. " +
                                "Please check your email to verify your account."
                );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @RequestParam("token") String token
    ) {

        emailVerificationService.verifyEmail(token);

        return ResponseEntity.ok(
                "Email verified successfully. You can now login."
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // NEW — Get current logged-in user
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getCurrentUser(
            Authentication authentication
    ) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + email)
                );

        MeResponse response = new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole() != null
                        ? user.getRole().getName()
                        : null
        );

        return ResponseEntity.ok(response);
    }
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(
                authentication.getName(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.ok("Password changed successfully.");
    }
}