package com.trustfund.service.implementServices;

import com.trustfund.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otp, String userName) {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("Email configuration is missing. Cannot send OTP email.");
            throw new IllegalStateException("Email service is not configured");
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset OTP - TrustFundME");

            String emailContent = buildOtpEmail(userName, otp);

            helper.setText(emailContent, true); // true = HTML content

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String buildOtpEmail(String userName, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }" +
                ".content { padding: 20px; background-color: #f9f9f9; }" +
                ".otp-box { background-color: #fff; border: 2px solid #4CAF50; border-radius: 5px; padding: 20px; margin: 20px 0; text-align: center; }" +
                ".otp-code { font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #4CAF50; font-family: 'Courier New', monospace; }" +
                ".warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                ".footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>Password Reset OTP</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hello " + (userName != null ? userName : "User") + ",</p>" +
                "<p>We received a request to reset your password for your TrustFundME account.</p>" +
                "<p>Please use the following OTP code to verify your identity:</p>" +
                "<div class='otp-box'>" +
                "<div class='otp-code'>" + otp + "</div>" +
                "</div>" +
                "<div class='warning'>" +
                "<p><strong>Important:</strong> This OTP will expire in 10 minutes. Do not share this code with anyone.</p>" +
                "</div>" +
                "<p>If you did not request a password reset, please ignore this email or contact support if you have concerns.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2024 TrustFundME. All rights reserved.</p>" +
                "<p>This is an automated email, please do not reply.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
