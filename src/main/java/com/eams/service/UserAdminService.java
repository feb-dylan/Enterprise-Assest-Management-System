package com.eams.service;

import com.eams.dto.request.Requests.InviteUserRequest;
import com.eams.dto.request.Requests.UpdateUserRequest;
import com.eams.dto.response.Responses.UserResponse;
import com.eams.entity.Employee;
import com.eams.entity.Role;
import com.eams.entity.User;
import com.eams.exception.BadRequestException;
import com.eams.exception.ConflictException;
import com.eams.exception.NotFoundException;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.RoleRepository;
import com.eams.repository.UserRepository;
import com.eams.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Admin user management. Previously there was no way to create a
 * MANAGER / TECHNICIAN / ADMIN account except editing the database.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final CurrentUserService currentUser;
    private final AuditService auditService;

    public record InviteResult(UserResponse user, String inviteLink) {}

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listByRole(String role) {
        return userRepository.findByRoleName(role).stream()
                .filter(User::isEnabled)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Creates the account with an unusable random password and emails
     * a one-time "set your password" link (valid 7 days).
     */
    public InviteResult invite(InviteUserRequest request) {
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with this email already exists");
        }
        Role role = findRole(request.role());

        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(Base64.getEncoder().encodeToString(random)))
                .role(role)
                .enabled(true)
                .emailVerified(false)
                .build();
        userRepository.save(user);

        String link = passwordResetService.createInvite(user);
        auditService.record(null, "USER", user.getId(), "INVITED",
                email + " invited as " + role.getName());
        return new InviteResult(toResponse(user), link);
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean self = Objects.equals(user.getId(), currentUser.userId());

        if (request.enabled() != null && request.enabled() != user.isEnabled()) {
            if (self && !request.enabled()) {
                throw new BadRequestException("You cannot disable your own account");
            }
            user.setEnabled(request.enabled());
            auditService.record(null, "USER", id, request.enabled() ? "ENABLED" : "DISABLED", user.getEmail());
        }

        if (request.role() != null && !request.role().equals(user.getRole().getName())) {
            if (self) {
                throw new BadRequestException("You cannot change your own role");
            }
            String old = user.getRole().getName();
            user.setRole(findRole(request.role()));
            // Old tokens still carry the old role - force a fresh login.
            user.setTokenVersion(user.getTokenVersion() + 1);
            auditService.record(null, "USER", id, "ROLE_CHANGED",
                    user.getEmail() + ": " + old + " -> " + request.role());
        }

        return toResponse(user);
    }

    private Role findRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new BadRequestException("Unknown role: " + name));
    }

    private UserResponse toResponse(User user) {
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().getName(),
                user.isEnabled(),
                employee != null ? employee.getId() : null,
                DtoMapper.fullName(employee),
                user.getCreatedAt());
    }
}
