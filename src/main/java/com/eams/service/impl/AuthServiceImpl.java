package com.eams.service.impl;

import com.eams.dto.request.RegisterRequest;
import com.eams.entity.Role;
import com.eams.entity.User;
import com.eams.repository.RoleRepository;
import com.eams.repository.UserRepository;
import com.eams.service.AuthService;
import com.eams.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eams.dto.request.LoginRequest;
import com.eams.dto.response.LoginResponse;
import com.eams.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public void register(RegisterRequest request) {

        // 1. Check whether email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        // 2. Find default role
        Role employeeRole = roleRepository.findByName("EMPLOYEE")
                .orElseThrow(() ->
                        new RuntimeException(
                                "EMPLOYEE role not found"
                        ));

        // 3. Create user
        User user = User.builder()
                .email(request.email())
                .password(
                        passwordEncoder.encode(request.password())
                )
                .role(employeeRole)
                .enabled(true)
                .emailVerified(false)
                .build();

        // 4. Save user first
        User savedUser = userRepository.save(user);

        // 5. Create verification token and send email
        emailVerificationService
                .createAndSendVerificationToken(savedUser.getId());
    }
    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password")
                );

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "Please verify your email before logging in"
            );
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getEmail(),
                user.getRole().getName()
        );
    }
}