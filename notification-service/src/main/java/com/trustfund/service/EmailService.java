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

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

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
                "<p>© 2026 TrustFundME Management System. Địa chỉ: Lô E2a-7, Đường D1, Khu Công nghệ cao, P.Tăng Nhơn Phú, TP.HCM. Đây là email tự động, vui lòng không trả lời.</p>" +
                "</div></div></body></html>";

        sendEmail(to, subject, htmlContent);
    }

    public void sendCommitmentRequestEmail(String to, String userName, String campaignTitle, Long campaignId) {
        String subject = "[YÊU CẦU] Ký bản cam kết trách nhiệm chiến dịch - TrustFundME";
        String signingUrl = frontendUrl + "/fund-owner/campaign/" + campaignId + "/commitment";
        
        String htmlContent = "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 10px;'>" +
                "<div style='background-color: #059669; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "<h1 style='margin: 0; font-size: 20px;'>YÊU CẦU KÝ CAM KẾT</h1>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<p>Kính gửi <strong>" + (userName != null ? userName : "Người dùng") + "</strong>,</p>" +
                "<p>Chiến dịch <strong>\"" + campaignTitle + "\"</strong> của bạn hiện đang ở bước soát xét cuối cùng. Để đảm bảo tính minh bạch và bảo vệ quyền lợi của các nhà hảo tâm, chúng tôi yêu cầu bạn thực hiện ký <strong>Bản cam kết trách nhiệm</strong> trước khi chiến dịch được chính thức phê duyệt.</p>" +
                "<div style='background-color: #f0fdf4; border-left: 4px solid #059669; padding: 15px; margin: 20px 0; text-align: center;'>" +
                "<p style='margin: 0 0 10px 0; font-weight: bold;'>Vui lòng thực hiện theo liên kết dưới đây:</p>" +
                "<a href='" + signingUrl + "' style='display: inline-block; background-color: #059669; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>KÝ CAM KẾT NGAY</a>" +
                "</div>" +
                "<p><strong>Nội dung chính của cam kết:</strong></p>" +
                "<ul>" +
                "<li>Đảm bảo sử dụng quỹ đúng mục đích đã đề ra.</li>" +
                "<li>Thực hiện cập nhật minh chứng chi tiêu đầy đủ và chính xác.</li>" +
                "<li>Chịu trách nhiệm hoàn toàn trước pháp luật nếu có hành vi gian lận.</li>" +
                "</ul>" +
                "</div>" +
                "<div style='padding: 20px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #777;'>" +
                "<p>© 2026 TrustFundME. Địa chỉ: Lô E2a-7, Đường D1, Khu Công nghệ cao, P.Tăng Nhơn Phú, TP.HCM.</p>" +
                "</div></div></body></html>";

        sendEmail(to, subject, htmlContent);
    }

    public void sendCommitmentSuccessEmail(String to, String userName, String campaignTitle) {
        String subject = "[XÁC NHẬN] Hoàn tất ký cam kết trách nhiệm - TrustFundME";
        
        String htmlContent = "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 10px;'>" +
                "<div style='background-color: #059669; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "<h1 style='margin: 0; font-size: 20px;'>KÝ KẾT THÀNH CÔNG</h1>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<p>Chúc mừng <strong>" + (userName != null ? userName : "Người dùng") + "</strong>,</p>" +
                "<p>Chúng tôi xác nhận bạn đã hoàn tất ký <strong>Bản cam kết trách nhiệm</strong> cho chiến dịch: <strong>\"" + campaignTitle + "\"</strong>.</p>" +
                "<p>Bản cam kết điện tử của bạn đã được lưu trữ an toàn trên hệ thống. Đây là bước quan trọng để chiến dịch của bạn tiến tới giai đoạn phê duyệt cuối cùng.</p>" +
                "<div style='background-color: #f0fdf4; border: 1px dashed #059669; padding: 15px; margin: 20px 0;'>" +
                "<p style='margin: 0; color: #065f46; font-weight: bold;'>Thông tin ghi nhận:</p>" +
                "<ul style='margin: 10px 0 0 0; font-size: 13px;'>" +
                "<li>Trạng thái: Đã ký (Signed)</li>" +
                "<li>Thời gian xác nhận: " + new java.util.Date().toLocaleString() + "</li>" +
                "<li>Nền tảng: TrustFundME e-Contract</li>" +
                "</ul>" +
                "</div>" +
                "<p>Cảm ơn bạn đã đồng hành cùng TrustFundME trong việc xây dựng cộng đồng thiện nguyện minh bạch.</p>" +
                "</div>" +
                "<div style='padding: 20px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #777;'>" +
                "<p>© 2026 TrustFundME. Đội ngũ quản trị chiến dịch.</p>" +
                "</div></div></body></html>";

        sendEmail(to, subject, htmlContent);
    }
}
