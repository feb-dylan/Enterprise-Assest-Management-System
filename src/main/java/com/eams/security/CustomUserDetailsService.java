package com.eams.security;

import com.eams.entity.EmployeeStatus;
import com.eams.entity.User;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * A user counts as disabled when:
     *  - the user account itself is disabled, or
     *  - the linked employee profile is INACTIVE.
     * This single rule is used both at login and on every request
     * (so deactivating someone takes effect immediately, not after 24h).
     */
    @Override
    @Transactional(readOnly = true)
    public AppUserPrincipal loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean employeeInactive = employeeRepository.findByUserId(user.getId())
                .map(e -> e.getStatus() == EmployeeStatus.INACTIVE)
                .orElse(false);

        return new AppUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().getName(),
                user.getTokenVersion(),
                user.isEnabled() && !employeeInactive
        );
    }
}
