package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.LoginRequest;
import com.trustfund.model.request.RegisterRequest;
import com.trustfund.model.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
}


