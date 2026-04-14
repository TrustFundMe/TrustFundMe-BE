package com.trustfund.service;

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
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String content) {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("Email configuration is missing. Cannot send email.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendLegalWarningEmail(String to, String userName, String campaignTitle, String content) {
        String subject = "[CẢNH BÁO PHÁP LÝ] Vi phạm cam kết trách nhiệm - TrustFundME";
        
        String htmlContent = "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 10px;'>" +
                "<div style='background-color: #dc2626; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "<h1 style='margin: 0; font-size: 20px;'>CẢNH BÁO VI PHẠM PHÁP LÝ</h1>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<p>Kính gửi <strong>" + (userName != null ? userName : "Người dùng") + "</strong>,</p>" +
                "<p>Hệ thống TrustFundME xin thông báo về việc vi phạm nghiêm trọng các điều khoản cam kết minh bạch tài chính đối với chiến dịch: <strong>\"" + campaignTitle + "\"</strong>.</p>" +
                "<div style='background-color: #fef2f2; border-left: 4px solid #dc2626; padding: 15px; margin: 20px 0;'>" +
                "<p style='margin: 0; color: #991b1b; font-weight: bold;'>Nội dung vi phạm:</p>" +
                "<p style='margin: 10px 0 0 0; color: #b91c1c;'>" + content + "</p>" +
                "</div>" +
                "<p>Căn cứ theo <strong>Bản cam kết trách nhiệm</strong> mà bạn đã ký kết trước khi thực hiện chiến dịch, chúng tôi đang hoàn tất các thủ tục pháp lý cần thiết để bàn giao hồ sơ cho cơ quan chức năng có thẩm quyền xử lý theo quy định của pháp luật Việt Nam.</p>" +
                "<p>Chúng tôi yêu cầu bạn thực hiện giải trình và bổ sung minh chứng chi tiêu ngay lập tức để giảm thiểu các hậu quả pháp lý có thể xảy ra.</p>" +
                "</div>" +
                "<div style='padding: 20px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #777;'>" +
                "<p>© 2024 TrustFundME Management System. Đây là email tự động, vui lòng không trả lời.</p>" +
                "</div></div></body></html>";

        sendEmail(to, subject, htmlContent);
    }
}
