package com.trustfund.client;

import com.trustfund.model.response.UserKYCResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EmailServiceClient {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public EmailServiceClient(RestTemplate restTemplate,
            @Value("${identity.service.url:http://localhost:8081}") String identityServiceUrl) {
        this.restTemplate = restTemplate;
        this.identityServiceUrl = identityServiceUrl.trim().replaceAll("/$", "");
    }

    /**
     * Gửi email yêu cầu ký cam kết qua identity-service.
     * KYC data (OCR fields) được truyền luôn để identity-service render email đầy đủ
     * mà không cần gọi ngược lại campaign-service.
     */
    public void sendCommitmentRequestEmail(
            String toEmail,
            String ownerName,
            String campaignTitle,
            Long campaignId,
            UserKYCResponse kycData,
            String frontendUrl) {
        String url = identityServiceUrl + "/api/emails/commitment-email";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("toEmail", toEmail);
            body.put("ownerName", ownerName != null ? ownerName : "Quý thành viên");
            body.put("campaignTitle", campaignTitle);
            body.put("campaignId", campaignId);
            body.put("frontendUrl", frontendUrl);
            if (kycData != null) {
                body.put("fullName", kycData.getFullName() != null ? kycData.getFullName() : "");
                body.put("address", kycData.getAddress() != null ? kycData.getAddress() : "");
                body.put("workplace", kycData.getWorkplace() != null ? kycData.getWorkplace() : "");
                body.put("taxId", kycData.getTaxId() != null ? kycData.getTaxId() : "");
                body.put("idNumber", kycData.getIdNumber() != null ? kycData.getIdNumber() : "");
                body.put("issueDate", kycData.getIssueDate() != null ? kycData.getIssueDate().toString() : "");
                body.put("issuePlace", kycData.getIssuePlace() != null ? kycData.getIssuePlace() : "");
                body.put("phoneNumber", kycData.getPhoneNumber() != null ? kycData.getPhoneNumber() : "");
            }
            restTemplate.postForEntity(url, body, Void.class);
            log.info("Commitment email sent to {} for campaign {}", toEmail, campaignId);
        } catch (Exception e) {
            log.error("Failed to send commitment email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send commitment email", e);
        }
    }

    public void sendCommitmentSuccessEmail(String toEmail, String ownerName, String campaignTitle) {
        String url = identityServiceUrl + "/api/emails/commitment-success-email";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("toEmail", toEmail);
            body.put("ownerName", ownerName);
            body.put("campaignTitle", campaignTitle);
            restTemplate.postForEntity(url, body, Void.class);
        } catch (Exception e) {
            log.warn("Failed to send commitment success email to {}: {}", toEmail, e.getMessage());
        }
    }
}
