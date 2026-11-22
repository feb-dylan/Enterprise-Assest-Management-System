package com.eams.service.impl;

import com.eams.config.AppProperties;
import com.eams.entity.EmailVerificationToken;
import com.eams.entity.User;
import com.eams.exception.BadRequestException;
import com.eams.exception.NotFoundException;
import com.eams.repository.EmailVerificationTokenRepository;
import com.eams.repository.UserRepository;
import com.eams.service.EmailService;
import com.eams.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Used only when REQUIRE_EMAIL_VERIFICATION=true.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AppProperties properties;

    @Override
    public void createAndSendVerificationToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationToken.setUsed(false);
        tokenRepository.save(verificationToken);

        String link = properties.backendUrl() + "/api/auth/verify-email?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), link);
    }

    @Override
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            throw new BadRequestException("Verification token has already been used");
        }
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired");
        }

        verificationToken.getUser().setEmailVerified(true);
        verificationToken.setUsed(true);
    }
}
