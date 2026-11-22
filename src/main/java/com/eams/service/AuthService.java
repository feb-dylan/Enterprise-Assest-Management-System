package com.eams.service;

import com.eams.dto.request.LoginRequest;
import com.eams.dto.request.RegisterRequest;
import com.eams.dto.response.LoginResponse;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    /**
     * Changes the password and invalidates every existing token for this user.
     * Returns a fresh token so the current session can continue.
     */
    LoginResponse changePassword(String email, String currentPassword, String newPassword);
}
