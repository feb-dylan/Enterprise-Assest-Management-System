package com.eams.service.impl;

import com.eams.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(
            String to,
            String verificationLink
    ) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Verify your EAMS account");

        message.setText(
                "Welcome to EAMS!\n\n" +
                        "Please verify your email address by clicking the link below:\n\n" +
                        verificationLink +
                        "\n\n" +
                        "This link will expire after 24 hours.\n\n" +
                        "If you did not create an EAMS account, you can ignore this email."
        );

        mailSender.send(message);
    }
}