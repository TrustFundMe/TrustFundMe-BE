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
    public void sendOtpEmail(String toEmail, String otp, String userName, String purpose) {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("Email configuration is missing. Cannot send OTP email.");
            throw new IllegalStateException("Email service is not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Verification Code - TrustFundME");
            helper.setText(buildOtpEmail(userName, otp, purpose), true);
            mailSender.send(message);
            log.info("OTP email sent successfully to: {} for purpose: {}", toEmail, purpose);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    @Override
    public void sendCommitmentRequestEmail(
            String toEmail, String ownerName, String campaignTitle, Long campaignId,
            String fullName, String address, String workplace, String taxId,
            String idNumber, String issueDate, String issuePlace, String phoneNumber) {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("Email configuration is missing. Cannot send commitment email.");
            throw new IllegalStateException("Email service is not configured");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[YÊU CẦU] Ký bản cam kết trách nhiệm chiến dịch - TrustFundME");
            String signingUrl = "http://localhost:3000/fund-owner/campaign/" + campaignId + "/commitment";
            helper.setText(buildCommitmentEmail(
                    ownerName, campaignTitle, signingUrl,
                    fullName, address, workplace, taxId,
                    idNumber, issueDate, issuePlace, phoneNumber), true);
            mailSender.send(message);
            log.info("Commitment email sent to: {} (fullName='{}', address='{}')", toEmail, fullName, address);
        } catch (MessagingException e) {
            log.error("Failed to send commitment email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send commitment email", e);
        }
    }

    @Override
    public void sendEvidenceReminder(String toEmail, String ownerName, String campaignTitle,
            String expenditurePlan, java.math.BigDecimal amount, java.time.LocalDateTime dueDate) {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("Email configuration is missing. Cannot send evidence reminder.");
            throw new IllegalStateException("Email service is not configured");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Nhắc nhở: Nộp minh chứng chi tiêu — " + campaignTitle);
            String dueDateStr = dueDate != null ? dueDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Chưa có hạn";
            String amountStr = amount != null ? String.format("%,.0f VND", amount.doubleValue()) : "—";
            helper.setText(buildEvidenceReminderEmail(ownerName, campaignTitle, expenditurePlan, amountStr, dueDateStr), true);
            mailSender.send(message);
            log.info("Evidence reminder email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send evidence reminder email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send evidence reminder email", e);
        }
    }

    @Override
    public void sendFraudReport(String toEmail, String ownerName, String campaignTitle, String reason, String evidence) {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.error("Email configuration is missing. Cannot send fraud report.");
            throw new IllegalStateException("Email service is not configured");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("BÁO CÁO LỪA ĐẢO — " + campaignTitle);
            helper.setText(buildFraudReportEmail(ownerName, campaignTitle, reason, evidence), true);
            mailSender.send(message);
            log.info("Fraud report email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send fraud report email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send fraud report email", e);
        }
    }

    @Override
    public void sendCommitmentSuccessEmail(String toEmail, String ownerName, String campaignTitle) {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.warn("Email configuration missing. Skipping success email.");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[XÁC NHẬN] Hoàn tất ký cam kết trách nhiệm - TrustFundME");
            helper.setText(buildCommitmentSuccessEmail(ownerName, campaignTitle), true);
            mailSender.send(message);
            log.info("Commitment success email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send commitment success email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── Email template builders ──────────────────────────────────────────────

    private String buildCommitmentEmail(
            String ownerName, String campaignTitle, String signingUrl,
            String fullName, String address, String workplace, String taxId,
            String idNumber, String issueDate, String issuePlace, String phoneNumber) {

        // Pre-fill party A info from OCR/KYC data
        String displayName = (fullName != null && !fullName.isBlank()) ? fullName : (ownerName != null ? ownerName : "Quý thành viên");
        String displayIdNumber = (idNumber != null && !idNumber.isBlank()) ? idNumber : "—";
        String displayIssueDate = (issueDate != null && !issueDate.isBlank()) ? issueDate : "—";
        String displayIssuePlace = (issuePlace != null && !issuePlace.isBlank()) ? issuePlace : "—";
        String displayAddress = (address != null && !address.isBlank()) ? address : "—";
        String displayWorkplace = (workplace != null && !workplace.isBlank()) ? workplace : "—";
        String displayTaxId = (taxId != null && !taxId.isBlank()) ? taxId : "—";
        String displayPhone = (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber : "—";

        return "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 700px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 10px;'>" +
                "<div style='background-color: #446b5f; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "<h1 style='margin: 0; font-size: 20px;'>YÊU CẦU KÝ CAM KẾT TRÁCH NHIỆM</h1>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<p>Xin chào <strong>" + displayName + "</strong>,</p>" +
                "<p>Chiến dịch <strong>\"" + campaignTitle + "\"</strong> của bạn đang ở bước soát xét cuối cùng. Để đảm bảo tính minh bạch và bảo vệ quyền lợi của các nhà hảo tâm, chúng tôi yêu cầu bạn thực hiện ký <strong>Bản cam kết trách nhiệm</strong> trước khi chiến dịch được chính thức phê duyệt.</p>" +

                // ── Che thông tin PII bảo mật ──
                "<div style='background-color: #f8f9fa; border: 1px solid #e0e0e0; border-radius: 8px; padding: 15px; margin: 15px 0;'>" +
                "<p style='margin: 0; color: #666; font-size: 14px;'>Tất cả thông tin định danh (KYC) của bạn bao gồm CCCD/Hộ chiếu đã được mã hóa theo chuẩn an toàn của hệ thống. Vui lòng nhấp vào liên kết bảo mật bên dưới để xem chi tiết bản gốc và tiến hành ký e-Contract.</p>" +
                "</div>" +

                "<div style='background-color: #f0fdf4; border-left: 4px solid #446b5f; padding: 15px; margin: 20px 0; text-align: center;'>" +
                "<p style='margin: 0 0 10px 0; font-weight: bold;'>Vui lòng xác nhận thông tin và ký bản cam kết:</p>" +
                "<a href='" + signingUrl + "' style='display: inline-block; background-color: #446b5f; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;'>KÝ CAM KẾT NGAY</a>" +
                "</div>" +

                "<p><strong>Nội dung chính của cam kết:</strong></p>" +
                "<ul>" +
                "<li>Đảm bảo sử dụng quỹ đúng mục đích đã đề ra trong hồ sơ chiến dịch.</li>" +
                "<li>Thực hiện cập nhật minh chứng chi tiêu đầy đủ và chính xác theo quy định.</li>" +
                "<li>Chịu trách nhiệm hoàn toàn trước pháp luật Việt Nam nếu có hành vi gian lận hoặc sử dụng sai mục đích.</li>" +
                "</ul>" +
                "</div>" +
                "<div style='padding: 15px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #777;'>" +
                "<p>© 2026 TrustFundMe — Hệ thống quản lý gây quỹ minh bạch.</p>" +
                "</div></div></body></html>";
    }

    private String buildCommitmentSuccessEmail(String ownerName, String campaignTitle) {
        String displayName = (ownerName != null) ? ownerName : "Quý thành viên";
        return "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 10px;'>" +
                "<div style='background-color: #059669; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "<h1 style='margin: 0; font-size: 20px;'>KÝ KẾT THÀNH CÔNG</h1>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<p>Chúc mừng <strong>" + displayName + "</strong>,</p>" +
                "<p>Bạn đã hoàn tất ký <strong>Bản cam kết trách nhiệm</strong> cho chiến dịch: <strong>\"" + campaignTitle + "\"</strong>.</p>" +
                "<div style='background-color: #f0fdf4; border: 1px dashed #059669; padding: 15px; margin: 20px 0;'>" +
                "<p style='margin: 0; color: #065f46; font-weight: bold;'>Thông tin ghi nhận:</p>" +
                "<ul style='margin: 10px 0 0 0; font-size: 13px;'>" +
                "<li>Trạng thái: <strong>Đã ký (Signed)</strong></li>" +
                "<li>Thời gian xác nhận: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "</li>" +
                "<li>Nền tảng: TrustFundMe e-Contract</li>" +
                "</ul>" +
                "</div>" +
                "<p>Chiến dịch của bạn đang chờ duyệt cuối cùng. Cảm ơn bạn đã đồng hành cùng TrustFundMe.</p>" +
                "</div>" +
                "<div style='padding: 20px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #777;'>" +
                "<p>© 2026 TrustFundMe.</p>" +
                "</div></div></body></html>";
    }

    private String buildEvidenceReminderEmail(String ownerName, String campaignTitle, String expenditurePlan, String amount, String dueDate) {
        return "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 10px;'>" +
                "<div style='background-color: #446b5f; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "<h1 style='margin: 0; font-size: 20px;'>Nhắc nhở nộp minh chứng chi tiêu</h1>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<p>Xin chào <strong>" + (ownerName != null ? ownerName : "Quý thành viên") + "</strong>,</p>" +
                "<p>Hệ thống TrustFundMe ghi nhận rằng bạn chưa nộp minh chứng chi tiêu cho đợt chi tiêu dưới đây:</p>" +
                "<div style='background-color: #fff; border-left: 4px solid #446b5f; padding: 15px; margin: 15px 0;'>" +
                "<p><strong>Chiến dịch:</strong> " + campaignTitle + "</p>" +
                "<p><strong>Đợt chi:</strong> " + expenditurePlan + "</p>" +
                "<p><strong>Số tiền:</strong> " + amount + "</p>" +
                "<p><strong>Hạn nộp:</strong> " + dueDate + "</p>" +
                "</div>" +
                "<div style='background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 15px 0;'>" +
                "<p><strong>Lưu ý:</strong> Vui lòng nộp hình ảnh hóa đơn, biên lai hoặc các tài liệu minh chứng khác càng sớm càng tốt. Nếu không nộp đúng hạn, chiến dịch và tài khoản của bạn có thể bị tạm ngưng theo quy định của TrustFundMe.</p>" +
                "</div>" +
                "</div>" +
                "<div style='padding: 20px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #777;'>" +
                "<p>© 2026 TrustFundMe.</p>" +
                "</div></div></body></html>";
    }

    private String buildFraudReportEmail(String ownerName, String campaignTitle, String reason, String evidence) {
        return "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 10px;'>" +
                "<div style='background-color: #991b1b; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "<h1 style='margin: 0; font-size: 20px;'>BÁO CÁO LỪA ĐẢO</h1>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<div style='background-color: #fee2e2; border-left: 4px solid #991b1b; padding: 15px; margin: 15px 0;'>" +
                "<p><strong>Cảnh báo:</strong> Chiến dịch dưới đây bị nghi ngờ lừa đảo hoặc vi phạm nghiêm trọng quy định TrustFundMe.</p>" +
                "</div>" +
                "<div style='background-color: #fff; border-left: 4px solid #446b5f; padding: 15px; margin: 15px 0;'>" +
                "<p><strong>Chủ quỹ:</strong> " + (ownerName != null ? ownerName : "Không xác định") + "</p>" +
                "<p><strong>Chiến dịch:</strong> " + campaignTitle + "</p>" +
                "<p><strong>Lý do:</strong> " + (reason != null ? reason : "Không có") + "</p>" +
                "<p><strong>Chi tiết:</strong> " + (evidence != null ? evidence : "Không có") + "</p>" +
                "</div>" +
                "<p>Chiến dịch đã bị khóa và bài cảnh báo đã được đăng lên trang cộng đồng. Đề nghị các cơ quan chức năng xem xét và xử lý theo quy định pháp luật.</p>" +
                "</div>" +
                "<div style='padding: 20px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #777;'>" +
                "<p>TrustFundMe — Báo cáo tự động</p>" +
                "</div></div></body></html>";
    }

    private String buildOtpEmail(String userName, String otp, String purpose) {
        String purposeText = "xác minh tài khoản của bạn";
        if ("verify_email".equals(purpose)) purposeText = "xác minh địa chỉ email của bạn";
        else if ("reset_password".equals(purpose)) purposeText = "đặt lại mật khẩu của bạn";

        return "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e1e1e1; border-radius: 10px;'>" +
                "<div style='background-color: #4CAF50; color: white; padding: 15px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                "<h1 style='margin: 0; font-size: 20px;'>Mã xác minh</h1>" +
                "</div>" +
                "<div style='padding: 20px;'>" +
                "<p>Xin chào <strong>" + (userName != null ? userName : "Người dùng") + "</strong>,</p>" +
                "<p>Mã xác minh của bạn:</p>" +
                "<div style='background-color: #f0fdf4; border: 2px solid #446b5f; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center; font-size: 28px; font-weight: bold; letter-spacing: 8px; color: #446b5f; font-family: Courier New, monospace;'>" + otp + "</div>" +
                "<div style='background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 12px; margin: 15px 0; font-size: 13px;'>" +
                "<strong>Lưu ý:</strong> Mã này có hiệu lực trong 10 phút. Vui lòng không chia sẻ mã cho bất kỳ ai.</div>" +
                "<p>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.</p>" +
                "</div>" +
                "<div style='padding: 20px; border-top: 1px solid #eeeeee; text-align: center; font-size: 12px; color: #777;'>" +
                "<p>© 2026 TrustFundMe.</p>" +
                "</div></div></body></html>";
    }
}