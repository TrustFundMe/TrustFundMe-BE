package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.LoginRequest;
import com.trustfund.model.request.RegisterRequest;
import com.trustfund.model.request.ResetPasswordRequest;
import com.trustfund.model.request.SendOtpRequest;
import com.trustfund.model.request.VerifyOtpRequest;
import com.trustfund.model.response.AuthResponse;
import com.trustfund.model.response.PasswordResetResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    PasswordResetResponse sendOtp(SendOtpRequest request);
    PasswordResetResponse verifyOtp(VerifyOtpRequest request);
    PasswordResetResponse resetPassword(ResetPasswordRequest request);
}


