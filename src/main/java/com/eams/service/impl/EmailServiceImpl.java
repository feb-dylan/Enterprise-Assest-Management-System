//package com.eams.service.impl;
//
//import com.eams.service.EmailService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class EmailServiceImpl implements EmailService {
//
//    private final JavaMailSender mailSender;
//
//    @Value("${app.mail.from}")
//    private String fromEmail;
//
//    @Override
//    public void sendVerificationEmail(
//            String to,
//            String verificationLink
//    ) {
//
//        SimpleMailMessage message = new SimpleMailMessage();
//
//        message.setFrom(fromEmail);
//        message.setTo(to);
//        message.setSubject("Verify your EAMS account");
//
//        message.setText(
//                "Welcome to EAMS!\n\n" +
//                        "Please verify your email address by clicking the link below:\n\n" +
//                        verificationLink +
//                        "\n\n" +
//                        "This link will expire after 24 hours.\n\n" +
//                        "If you did not create an EAMS account, you can ignore this email."
//        );
//
//        mailSender.send(message);
//    }
//}

package com.eams.service.impl;

import com.eams.service.EmailService;
import jakarta.annotation.PostConstruct;
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

    @Value("${spring.mail.username}")
    private String smtpUsername;

    @Value("${spring.mail.password}")
    private String smtpPassword;

    @PostConstruct
    public void checkMailConfig() {

        System.out.println("========== SMTP CONFIG CHECK ==========");
        System.out.println("SMTP username: " + smtpUsername);

        if (smtpPassword == null) {
            System.out.println("SMTP password: NULL");
        } else {
            System.out.println(
                    "SMTP password length: " + smtpPassword.length()
            );

            System.out.println(
                    "SMTP password has surrounding whitespace: "
                            + !smtpPassword.equals(smtpPassword.trim())
            );
        }

        System.out.println("=======================================");
    }

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