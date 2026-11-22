package com.eams.service;

import com.eams.config.AppProperties;
import com.eams.entity.PasswordResetToken;
import com.eams.entity.TokenPurpose;
import com.eams.entity.User;
import com.eams.exception.BadRequestException;
import com.eams.repository.PasswordResetTokenRepository;
import com.eams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Forgot-password and invitation links.
 *
 * - The raw token only exists in the email link; the database stores a SHA-256 hash.
 * - Reset links expire after 30 minutes, invitations after 7 days. Single use.
 * - "Forgot password" always answers the same way, so it cannot be used
 *   to find out which emails have accounts.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuditService auditService;
    private final AppProperties properties;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            if (!user.isEnabled()) {
                return;
            }
            String token = createToken(user, TokenPurpose.PASSWORD_RESET, 30);
            String link = properties.frontendUrl() + "/reset-password?token=" + token;
            mailService.send(user.getEmail(), "[EAMS] Reset your password",
                    "We received a request to reset your EAMS password.\n\n"
                            + "Open this link within 30 minutes to choose a new password:\n"
                            + link + "\n\n"
                            + "If you did not request this, you can ignore this email.");
            if (!mailService.isEnabled()) {
                // Development only: mail is off, so print the link to the server log.
                log.info("Password reset link for {} (mail disabled): {}", user.getEmail(), link);
            }
        });
    }

    /** Creates an invitation link for a user an admin just created. Returns the raw link. */
    @Transactional
    public String createInvite(User user) {
        String token = createToken(user, TokenPurpose.INVITE, 7 * 24 * 60);
        String link = properties.frontendUrl() + "/reset-password?token=" + token + "&invite=1";
        mailService.send(user.getEmail(), "[EAMS] You have been invited",
                "An administrator created an EAMS account for you (role: "
                        + user.getRole().getName() + ").\n\n"
                        + "Open this link within 7 days to set your password:\n"
                        + link);
        return link;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid or expired link"));

        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired link");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setEmailVerified(true);
        token.setUsed(true);

        auditService.record(null, "USER", user.getId(),
                token.getPurpose() == TokenPurpose.INVITE ? "INVITE_ACCEPTED" : "PASSWORD_RESET",
                null);
    }

    // ---------------------------------------------------------

    private String createToken(User user, TokenPurpose purpose, int validMinutes) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setTokenHash(hash(raw));
        token.setUser(user);
        token.setPurpose(purpose);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(validMinutes));
        tokenRepository.save(token);
        return raw;
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
