package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.LoginRequest;
import com.trustfund.model.request.RegisterRequest;
import com.trustfund.model.request.ResetPasswordRequest;
import com.trustfund.model.request.SendOtpRequest;
import com.trustfund.model.request.VerifyEmailRequest;
import com.trustfund.model.request.VerifyOtpRequest;
import com.trustfund.model.response.AuthResponse;
import com.trustfund.model.response.PasswordResetResponse;
import com.trustfund.model.request.GoogleLoginRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    PasswordResetResponse sendOtp(SendOtpRequest request);
    PasswordResetResponse verifyOtp(VerifyOtpRequest request);
    PasswordResetResponse verifyEmail(VerifyEmailRequest request);
    PasswordResetResponse resetPassword(ResetPasswordRequest request);
    AuthResponse googleLogin(GoogleLoginRequest request);
}


