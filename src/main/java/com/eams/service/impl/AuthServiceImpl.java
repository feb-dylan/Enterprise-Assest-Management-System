package com.eams.service.impl;

import com.eams.config.AppProperties;
import com.eams.dto.request.LoginRequest;
import com.eams.dto.request.RegisterRequest;
import com.eams.dto.response.LoginResponse;
import com.eams.entity.Role;
import com.eams.entity.User;
import com.eams.exception.BadRequestException;
import com.eams.exception.ConflictException;
import com.eams.exception.ForbiddenException;
import com.eams.exception.NotFoundException;
import com.eams.repository.RoleRepository;
import com.eams.repository.UserRepository;
import com.eams.security.JwtService;
import com.eams.security.LoginAttemptService;
import com.eams.service.AuditService;
import com.eams.service.AuthService;
import com.eams.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final EmailVerificationService emailVerificationService;
    private final AuditService auditService;
    private final AppProperties properties;

    // =========================================================
    // REGISTER (self sign-up, can be turned off: PUBLIC_REGISTRATION=false)
    // =========================================================

    @Override
    public void register(RegisterRequest request) {
        if (!properties.auth().publicRegistration()) {
            throw new ForbiddenException(
                    "Self-registration is disabled. Ask an administrator to invite you.");
        }

        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already exists");
        }

        Role employeeRole = roleRepository.findByName("EMPLOYEE")
                .orElseThrow(() -> new IllegalStateException("EMPLOYEE role not found"));

        boolean requireVerification = properties.auth().requireEmailVerification();

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(employeeRole)
                .enabled(true)
                .emailVerified(!requireVerification)
                .build();
        userRepository.save(user);

        if (requireVerification) {
            emailVerificationService.createAndSendVerificationToken(user.getId());
        }
        auditService.record(null, "USER", user.getId(), "REGISTERED", email);
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();
        loginAttemptService.checkAllowed(email);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(email);
            throw e;
        } catch (DisabledException e) {
            // Spring checks "disabled" BEFORE the password. Only reveal that the
            // account is disabled to someone who knows the correct password.
            boolean passwordOk = userRepository.findByEmail(email)
                    .map(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                    .orElse(false);
            if (!passwordOk) {
                loginAttemptService.recordFailure(email);
                throw new BadCredentialsException("Bad credentials");
            }
            throw e;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));

        if (properties.auth().requireEmailVerification() && !user.isEmailVerified()) {
            throw new ForbiddenException("Please verify your email address before logging in");
        }

        loginAttemptService.recordSuccess(email);
        return new LoginResponse(jwtService.generateToken(user), user.getEmail(), user.getRole().getName());
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @Override
    public LoginResponse changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        // Logs out every other device/session that still has an old token.
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        auditService.record(null, "USER", user.getId(), "PASSWORD_CHANGED", null);
        return new LoginResponse(jwtService.generateToken(user), user.getEmail(), user.getRole().getName());
    }
}
