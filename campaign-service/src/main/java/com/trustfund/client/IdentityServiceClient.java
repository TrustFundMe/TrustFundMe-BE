package com.trustfund.client;

import com.trustfund.model.response.UserInfoResponse;
import com.trustfund.model.response.UserVerificationStatusResponse;
import com.trustfund.model.response.UserKYCResponse;
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
            Long[] ids = restTemplate.getForObject(url, Long[].class);
            return ids != null ? java.util.Arrays.asList(ids) : java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch staff IDs: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Cập nhật điểm uy tín của user qua identity-service.
     */
    public void updateTrustScore(Long userId, int delta) {
        if (userId == null) return;
        String url = identityServiceUrl + "/api/internal/users/" + userId + "/update-trust-score?delta=" + delta;
        try {
            restTemplate.put(url, null);
            log.info("➔ Successfully synced trust score delta {} for user {} to Identity Service", delta, userId);
        } catch (Exception e) {
            log.error("❌ Failed to sync trust score for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Lấy BXH điểm uy tín từ identity-service.
     */
    public java.util.List<UserInfoResponse> getLeaderboard(int page, int size) {
        String url = identityServiceUrl + "/api/internal/users/leaderboard?page=" + page + "&size=" + size;
        try {
            log.debug("Fetching leaderboard from {}", url);
            UserInfoResponse[] users = restTemplate.getForObject(url, UserInfoResponse[].class);
            return users != null ? java.util.Arrays.asList(users) : java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch leaderboard: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public UserInfoResponse getUserById(Long userId) {
        if (userId == null) return null;
        String url = identityServiceUrl + "/api/internal/users/" + userId;
        try {
            log.debug("Calling identity-service for user {}: {}", userId, url);
            UserInfoResponse response = restTemplate.getForObject(url, UserInfoResponse.class);
            if (response == null) {
                log.warn("Identity service returned null for user {}", userId);
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to get user {} from {}: {}", userId, url, e.getMessage());
            return null;
        }
    }

    public UserKYCResponse getUserKYC(Long userId) {
        if (userId == null) return null;
        String url = identityServiceUrl + "/api/internal/users/" + userId + "/kyc";
        try {
            return restTemplate.getForObject(url, UserKYCResponse.class);
        } catch (Exception e) {
            log.error("Failed to get KYC for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public void banUser(Long userId, String reason) {
        if (userId == null) return;
        String url = identityServiceUrl + "/api/internal/users/" + userId + "/ban";
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("reason", reason != null ? reason : "Vi phạm quy định");
            restTemplate.put(url, body);
            log.info("User {} banned successfully", userId);
        } catch (Exception e) {
            log.error("Failed to ban user {}: {}", userId, e.getMessage());
        }
    }

    public void unbanUser(Long userId) {
        if (userId == null) return;
        String url = identityServiceUrl + "/api/internal/users/" + userId + "/unban";
        try {
            restTemplate.put(url, null);
            log.info("User {} unbanned successfully", userId);
        } catch (Exception e) {
            log.error("Failed to unban user {}: {}", userId, e.getMessage());
        }
    }
}
