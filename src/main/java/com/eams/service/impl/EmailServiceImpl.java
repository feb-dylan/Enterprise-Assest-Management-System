package com.eams.service.impl;

import com.eams.service.EmailService;
import com.eams.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final MailService mailService;

    @Override
    public void sendVerificationEmail(String to, String verificationLink) {
        mailService.send(to, "Verify your EAMS account",
                "Welcome to EAMS!\n\n"
                        + "Please verify your email address by clicking the link below:\n\n"
                        + verificationLink + "\n\n"
                        + "This link will expire after 24 hours.\n\n"
                        + "If you did not create an EAMS account, you can ignore this email.");
    }
}
