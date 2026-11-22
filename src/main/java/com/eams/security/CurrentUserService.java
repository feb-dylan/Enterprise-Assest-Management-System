package com.eams.security;

import com.eams.entity.Employee;
import com.eams.entity.User;
import com.eams.exception.ForbiddenException;
import com.eams.exception.NotFoundException;
import com.eams.exception.UnauthorizedException;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Single source of truth for "who is calling this API".
 *
 * Identity ALWAYS comes from the verified JWT, never from an id the
 * client puts in the URL or request body.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public AppUserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal p)) {
            throw new UnauthorizedException("Authentication required");
        }
        return p;
    }

    public Optional<AppUserPrincipal> optionalPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public Long userId() {
        return principal().id();
    }

    public String role() {
        return principal().role();
    }

    public boolean hasRole(String role) {
        return optionalPrincipal().map(p -> p.role().equalsIgnoreCase(role)).orElse(false);
    }

    public boolean isEmployeeRole() {
        return hasRole("EMPLOYEE");
    }

    public User user() {
        return userRepository.findById(userId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
    }

    public Optional<Employee> optionalEmployee() {
        return employeeRepository.findByUserId(userId());
    }

    /** The employee profile linked to the logged-in user (404 if not created yet). */
    public Employee employee() {
        return optionalEmployee().orElseThrow(() -> new NotFoundException(
                "Employee profile not found. Please complete your employee profile first."));
    }

    /**
     * Backward compatibility for old URLs like /api/requests/my/{employeeId}.
     * The id in the URL must belong to the caller, otherwise 403.
     */
    public Employee requireSelf(Long employeeIdFromPath) {
        Employee me = employee();
        if (employeeIdFromPath != null && !Objects.equals(me.getId(), employeeIdFromPath)) {
            throw new ForbiddenException("You can only access your own records");
        }
        return me;
    }
}
