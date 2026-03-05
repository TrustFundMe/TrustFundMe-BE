package com.trustfund.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class UserInfoClient {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    private final Map<Long, UserInfo> cache = new ConcurrentHashMap<>();

    public UserInfoClient(RestTemplate restTemplate,
                          @Value("${identity.service.url:http://localhost:8081}") String identityServiceUrl) {
        this.restTemplate = restTemplate;
        this.identityServiceUrl = identityServiceUrl;
    }

    public UserInfo getUserInfo(Long userId) {
        if (userId == null) return UserInfo.empty();
        return cache.computeIfAbsent(userId, this::fetchUserInfo);
    }

    private UserInfo fetchUserInfo(Long userId) {
        try {
            String url = identityServiceUrl + "/api/internal/users/" + userId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return UserInfo.empty();
            String fullName = (String) response.get("fullName");
            String avatarUrl = (String) response.get("avatarUrl");
            return new UserInfo(fullName, avatarUrl);
        } catch (Exception e) {
            log.warn("Could not fetch user info for userId={}: {}", userId, e.getMessage());
            return UserInfo.empty();
        }
    }

    public record UserInfo(String fullName, String avatarUrl) {
        public static UserInfo empty() {
            return new UserInfo(null, null);
        }
    }
}
