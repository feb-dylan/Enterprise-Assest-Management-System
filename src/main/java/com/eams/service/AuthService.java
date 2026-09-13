package com.eams.service;

import com.eams.dto.request.LoginRequest;
import com.eams.dto.request.RegisterRequest;
import com.eams.dto.response.LoginResponse;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void changePassword(String email, String currentPassword, String newPassword);
}