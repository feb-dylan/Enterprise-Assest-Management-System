package com.eams.service;

import com.eams.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text email in a background thread.
 * A slow or broken SMTP server can never slow down or break an API call.
 * Disabled unless MAIL_ENABLED=true.
 */
@Service
@RequiredArgsConstructor
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties properties;

    public boolean isEnabled() {
        return properties.mail() != null
                && properties.mail().enabled()
                && mailSender.getIfAvailable() != null;
    }

    @Async
    public void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }
        if (!isEnabled()) {
            log.debug("Mail disabled - skipped email to {}: {}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.mail().from());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.getObject().send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
