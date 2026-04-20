package com.trustfund.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp, String userName, String purpose);
    void sendCommitmentRequestEmail(
            String toEmail, String ownerName, String campaignTitle, Long campaignId,
            String fullName, String address, String workplace, String taxId,
            String idNumber, String issueDate, String issuePlace, String phoneNumber,
            String frontendUrl);
    void sendEvidenceReminder(String toEmail, String ownerName, String campaignTitle, String expenditurePlan, java.math.BigDecimal amount, java.time.LocalDateTime dueDate);
    void sendFraudReport(String toEmail, String ownerName, String campaignTitle, String reason, String evidence);
    void sendCommitmentSuccessEmail(String toEmail, String ownerName, String campaignTitle);
}