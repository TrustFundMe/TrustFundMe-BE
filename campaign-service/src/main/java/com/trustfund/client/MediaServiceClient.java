package com.trustfund.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Slf4j
@Component
public class MediaServiceClient {

    private final RestTemplate restTemplate;
    private final String mediaServiceUrl;

    public MediaServiceClient(RestTemplate restTemplate,
            @Value("${media.service.url:http://localhost:8083}") String mediaServiceUrl) {
        this.restTemplate = restTemplate;
        this.mediaServiceUrl = mediaServiceUrl.trim().replaceAll("/$", "");
    }

    /**
     * Resolves the media URL from media ID by calling media-service.
     */
    public String getMediaUrl(Long mediaId) {
        if (mediaId == null)
            return null;

        String url = mediaServiceUrl + "/api/media/" + mediaId;
        try {
            log.debug("Fetching media URL for mediaId {} from {}", mediaId, url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("url")) {
                return (String) response.get("url");
            }
        } catch (Exception e) {
            log.error("Failed to fetch media URL for mediaId {}: {}", mediaId, e.getMessage());
        }
        return null; // Graceful fallback
    }
}
