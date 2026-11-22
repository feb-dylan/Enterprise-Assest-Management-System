package com.eams.controller;

import com.eams.dto.request.ChangePasswordRequest;
import com.eams.dto.request.LoginRequest;
import com.eams.dto.request.RegisterRequest;
import com.eams.dto.request.Requests.ForgotPasswordRequest;
import com.eams.dto.request.Requests.ResetPasswordRequest;
import com.eams.dto.response.LoginResponse;
import com.eams.dto.response.MeResponse;
import com.eams.dto.response.Responses.MessageResponse;
import com.eams.security.AppUserPrincipal;
import com.eams.security.CurrentUserService;
import com.eams.service.AuthService;
import com.eams.service.EmailVerificationService;
import com.eams.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final CurrentUserService currentUser;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Registration successful. You can now log in.");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully. You can now login.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** The logged-in user, now including employeeId so the frontend does not need a second call. */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        AppUserPrincipal p = currentUser.principal();
        MeResponse base = new MeResponse(p.id(), p.email(), p.role());
        Long employeeId = currentUser.optionalEmployee().map(e -> e.getId()).orElse(null);
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("id", base.getId());
        body.put("email", base.getEmail());
        body.put("role", base.getRole());
        body.put("employeeId", employeeId);
        return ResponseEntity.ok(body);
    }

    /**
     * Changes the password. All older tokens stop working (other devices are logged out);
     * the response contains a fresh token for this session.
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        LoginResponse login = authService.changePassword(
                currentUser.principal().email(),
                request.getCurrentPassword(),
                request.getNewPassword());
        return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully.",
                "token", login.token()));
    }

    /** Always returns the same answer, so it cannot reveal which emails exist. */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(new MessageResponse(
                "If an account exists for that email, a reset link has been sent."));
    }

    /** Used for both "forgot password" links and invitation links. */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse("Password updated. You can now log in."));
    }
}
