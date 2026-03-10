package com.trustfund.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class FlagServiceClient {

    private final RestTemplate restTemplate;
    private final String flagServiceUrl;

    public FlagServiceClient(RestTemplate restTemplate,
                             @Value("${flag.service.url:http://localhost:8085}") String flagServiceUrl) {
        this.restTemplate = restTemplate;
        this.flagServiceUrl = flagServiceUrl;
    }

    public int getFlagCountForPost(Long postId) {
        try {
            String url = flagServiceUrl + "/api/flags/posts/" + postId + "?page=0&size=1";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("totalElements")) {
                return ((Number) response.get("totalElements")).intValue();
            }
            return 0;
        } catch (Exception e) {
            log.warn("Could not fetch flag count for postId={}: {}", postId, e.getMessage());
            return 0;
        }
    }
}
