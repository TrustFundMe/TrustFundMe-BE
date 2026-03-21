package com.trustfund.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    public String getMediaUrl(Long coverImage) {
        if (coverImage == null)
            return null;

        String url = mediaServiceUrl + "/api/media/" + coverImage;
        try {
            log.debug("Fetching media URL for coverImage {} from {}", coverImage, url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("url")) {
                return (String) response.get("url");
            }
            log.warn("Media response for ID {} did not contain a URL. Response: {}", coverImage, response);
        } catch (Exception e) {
            log.error("Failed to fetch media URL for coverImage {} at {}: {}", coverImage, url, e.getMessage());
        }
        return null;
    }

    /**
     * Lấy URL ảnh đầu tiên của chiến dịch làm ảnh bìa dự phòng.
     */
    public String getFirstImageByCampaignId(Long campaignId) {
        if (campaignId == null) return null;
        String url = mediaServiceUrl + "/api/media/campaigns/" + campaignId + "/first-image";
        try {
            log.debug("Fetching fallback cover image for campaign {} from {}", campaignId, url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("url")) {
                return (String) response.get("url");
            }
        } catch (Exception e) {
            log.warn("No fallback cover image found for campaign {}: {}", campaignId, e.getMessage());
        }
        return null;
    }

    /**
     * Fetch all media files associated with a post.
     * Note: this method returns raw JSON maps to avoid tight coupling with media-service DTOs.
     */
    public List<Map<String, Object>> getMediaByPostId(Long postId) {
        if (postId == null) return Collections.emptyList();

        String url = mediaServiceUrl + "/api/media/posts/" + postId;
        try {
            log.debug("Fetching media for post {} from {}", postId, url);
            Map<String, Object>[] response = restTemplate.getForObject(url, Map[].class);
            if (response == null) return Collections.emptyList();
            return Arrays.asList(response);
        } catch (Exception e) {
            log.error("Failed to fetch media for postId {} at {}: {}", postId, url, e.getMessage());
            return Collections.emptyList();
        }
    }
}
