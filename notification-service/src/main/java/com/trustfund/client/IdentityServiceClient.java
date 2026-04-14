package com.trustfund.client;

import com.trustfund.dto.UserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

    public UserInfoResponse getUserInfo(Long userId) {
        if (userId == null) return null;
        String url = identityServiceUrl + "/api/internal/users/" + userId;
        try {
            log.debug("Fetching user info for user {} from {}", userId, url);
            return restTemplate.getForObject(url, UserInfoResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch user info for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
