package com.trustfund.client;

import com.trustfund.model.response.UserInfoResponse;
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

    /**
     * Gọi identity-service để nâng cấp role của user lên FUND_OWNER.
     */
    public void upgradeUserRole(Long userId) {
        if (userId == null)
            return;

        String url = identityServiceUrl + "/api/internal/users/" + userId + "/upgrade-role";
        try {
            restTemplate.put(url, null);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to upgrade user role. Please ensure Identity Service is running.");
        }
    }

    /**
     * Lấy thông tin user từ identity-service.
     */
    public UserInfoResponse getUserInfo(Long userId) {
        if (userId == null)
            return null;

        String url = identityServiceUrl + "/api/internal/users/" + userId;
        try {
            log.debug("Fetching user info for user {} from {}", userId, url);
            return restTemplate.getForObject(url, UserInfoResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch user info for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tài khoản ngân hàng chính của user từ identity-service.
     */
    public com.trustfund.model.response.BankAccountResponse getPrimaryBankAccount(Long userId) {
        if (userId == null)
            return null;

        String url = identityServiceUrl + "/api/internal/users/" + userId + "/primary-bank";
        try {
            log.debug("Fetching primary bank account for user {} from {}", userId, url);
            return restTemplate.getForObject(url, com.trustfund.model.response.BankAccountResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch primary bank account for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Lấy danh sách ID của tất cả nhân viên từ identity-service.
     */
    public java.util.List<Long> getStaffIds() {
        String url = identityServiceUrl + "/api/internal/users/staff-ids";
        try {
            log.debug("Fetching staff IDs from {}", url);
            Integer[] ids = restTemplate.getForObject(url, Integer[].class);
            return ids != null ? java.util.Arrays.stream(ids).map(Integer::longValue).collect(java.util.stream.Collectors.toList()) : java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch staff IDs: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}
