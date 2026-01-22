package com.trustfund.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp, String userName);
}
