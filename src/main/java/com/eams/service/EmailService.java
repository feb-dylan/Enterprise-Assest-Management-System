package com.eams.service;

public interface EmailService {

    void sendVerificationEmail(String to, String verificationLink);
}