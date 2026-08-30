package com.eams.service.impl;

import com.eams.entity.EmailVerificationToken;
import com.eams.entity.User;
import com.eams.repository.EmailVerificationTokenRepository;
import com.eams.repository.UserRepository;
import com.eams.service.EmailService;
import com.eams.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationServiceImpl
        implements EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    public void createAndSendVerificationToken(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Remove previous verification tokens
        tokenRepository.deleteByUser(user);

        // Generate secure random token
        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                new EmailVerificationToken();

        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(
                LocalDateTime.now().plusHours(24)
        );
        verificationToken.setUsed(false);

        tokenRepository.save(verificationToken);

        String verificationLink =
                "http://localhost:8080/api/auth/verify-email?token="
                        + token;

        emailService.sendVerificationEmail(
                user.getEmail(),
                verificationLink
        );
    }

    @Override
    public void verifyEmail(String token) {

        EmailVerificationToken verificationToken =
                tokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid verification token"
                                ));

        if (verificationToken.isUsed()) {
            throw new RuntimeException(
                    "Verification token has already been used"
            );
        }

        if (verificationToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Verification token has expired"
            );
        }

        User user = verificationToken.getUser();

        user.setEmailVerified(true);

        verificationToken.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(verificationToken);
    }
}