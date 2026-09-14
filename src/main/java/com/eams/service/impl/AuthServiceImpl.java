package com.eams.service.impl;

import com.eams.dto.request.LoginRequest;
import com.eams.dto.request.RegisterRequest;
import com.eams.dto.response.LoginResponse;
import com.eams.entity.Employee;
import com.eams.entity.EmployeeStatus;
import com.eams.entity.Role;
import com.eams.entity.User;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.RoleRepository;
import com.eams.repository.UserRepository;
import com.eams.security.JwtService;
import com.eams.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // =========================================================
    // REGISTER
    // =========================================================

    @Override
    public void register(RegisterRequest request) {

        // -----------------------------------------------------
        // 1. Check email
        // -----------------------------------------------------

        if (userRepository.existsByEmail(request.email())) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        // -----------------------------------------------------
        // 2. Find default EMPLOYEE role
        // -----------------------------------------------------

        Role employeeRole =
                roleRepository
                        .findByName("EMPLOYEE")
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "EMPLOYEE role not found"
                                )
                        );

        // -----------------------------------------------------
        // 3. Create user
        // -----------------------------------------------------

        User user = User.builder()
                .email(request.email())
                .password(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .role(employeeRole)
                .enabled(true)
                .emailVerified(true)
                .build();

        // -----------------------------------------------------
        // 4. Save
        // -----------------------------------------------------

        userRepository.save(user);
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Override
    public LoginResponse login(
            LoginRequest request) {

        // -----------------------------------------------------
        // 1. Find user
        // -----------------------------------------------------

        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid email or password"
                                )
                        );

        // -----------------------------------------------------
        // 2. Check USER account enabled
        // -----------------------------------------------------

        if (!user.isEnabled()) {

            throw new RuntimeException(
                    "Account is disabled"
            );
        }

        // -----------------------------------------------------
        // 3. Check EMPLOYEE status
        //
        // Only employees have EmployeeStatus.
        //
        // MANAGER / ADMIN are not blocked by employee status.
        // -----------------------------------------------------

        if (user.getRole() != null &&
                "EMPLOYEE".equalsIgnoreCase(
                        user.getRole().getName()
                )) {

            Employee employee =
                    employeeRepository
                            .findByUserEmail(
                                    user.getEmail()
                            )
                            .orElse(null);

            /*
             * If employee profile exists and is INACTIVE,
             * login is blocked.
             *
             * If profile does not exist yet, login is allowed
             * because the employee may still need to create
             * their employee profile.
             */

            if (employee != null &&
                    employee.getStatus() ==
                            EmployeeStatus.INACTIVE) {

                throw new RuntimeException(
                        "Your employee account is inactive"
                );
            }
        }

        // -----------------------------------------------------
        // 4. Authenticate email + password
        // -----------------------------------------------------

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // -----------------------------------------------------
        // 5. Generate JWT
        // -----------------------------------------------------

        String token =
                jwtService.generateToken(user);

        // -----------------------------------------------------
        // 6. Return login response
        // -----------------------------------------------------

        return new LoginResponse(
                token,
                user.getEmail(),
                user.getRole().getName()
        );
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @Override
    public void changePassword(
            String email,
            String currentPassword,
            String newPassword
    ) {

        // -----------------------------------------------------
        // 1. Find current user
        // -----------------------------------------------------

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        // -----------------------------------------------------
        // 2. Verify current password
        // -----------------------------------------------------

        if (!passwordEncoder.matches(
                currentPassword,
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }

        // -----------------------------------------------------
        // 3. New password cannot be same
        // -----------------------------------------------------

        if (passwordEncoder.matches(
                newPassword,
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "New password must be different from current password"
            );
        }

        // -----------------------------------------------------
        // 4. Save new password
        // -----------------------------------------------------

        user.setPassword(
                passwordEncoder.encode(
                        newPassword
                )
        );

        userRepository.save(user);
    }
}