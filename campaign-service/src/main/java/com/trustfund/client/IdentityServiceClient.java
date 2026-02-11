package com.trustfund.client;

import com.trustfund.model.response.UserVerificationStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class IdentityServiceClient {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public IdentityServiceClient(RestTemplate restTemplate,
            @Value("${identity.service.url:http://localhost:8081}") String identityServiceUrl) {
        this.restTemplate = restTemplate;
        this.identityServiceUrl = identityServiceUrl.trim().replaceAll("/$", "");
    }

    /**
     * Gọi identity-service để kiểm tra user có tồn tại không.
     * Nếu không tồn tại hoặc không kết nối được -> ném 400.
     */
    public void validateUserExists(Long userId) {
        if (userId == null)
            return;

        String url = identityServiceUrl + "/api/internal/users/" + userId + "/exists";
        try {
            restTemplate.getForEntity(url, Void.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User {} not found in Identity Service", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fund owner with id " + userId + " not found");
        } catch (Exception e) {
            log.error("Error validating user {} in Identity Service: {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unable to validate fund owner id. Please ensure Identity Service is running.");
        }
    }

    /**
     * Lấy trạng thái xác thực KYC và Bank Account của user từ identity-service.
     */
    public UserVerificationStatusResponse getVerificationStatus(Long userId) {
        if (userId == null)
            return null;

        String url = identityServiceUrl + "/api/internal/users/" + userId + "/verification-status";
        try {
            log.debug("Fetching verification status for user {} from {}", userId, url);
            return restTemplate.getForObject(url, UserVerificationStatusResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch verification status for user {}: {}", userId, e.getMessage());
            return UserVerificationStatusResponse.builder()
                    .kycVerified(false)
                    .bankVerified(false)
                    .build();
        }
    }
}
