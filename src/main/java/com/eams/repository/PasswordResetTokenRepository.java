package com.eams.repository;

import com.eams.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    @EntityGraph(attributePaths = {"user", "user.role"})
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
