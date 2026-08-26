package com.eams.service;

public interface EmailVerificationService {

    void createAndSendVerificationToken(Long userId);

    void verifyEmail(String token);
}