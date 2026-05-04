package com.trustfund.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MediaServiceClient {

    private final RestTemplate restTemplate;
    private final String mediaServiceUrl;

    // In-memory cache: mediaId → url (or "" for failed lookups). TTL = 60s.
    private final ConcurrentHashMap<Long, CachedValue> mediaUrlCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CachedValue> campaignImageCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000;

    private static class CachedValue {
        final String value;
        final long timestamp;
        CachedValue(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public MediaServiceClient(RestTemplate restTemplate,
            @Value("${media.service.url:http://localhost:8083}") String mediaServiceUrl) {
        this.restTemplate = restTemplate;
        this.mediaServiceUrl = mediaServiceUrl.trim().replaceAll("/$", "");
    }

    /**
     * Resolves the media URL from media ID by calling media-service.
     * Results are cached for 60s (including failed lookups) to avoid repeated slow calls.
     */
    public String getMediaUrl(Long coverImage) {
        if (coverImage == null)
            return null;

        // Check cache first — avoid repeated HTTP calls for same ID
        CachedValue cached = mediaUrlCache.get(coverImage);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }

        String url = mediaServiceUrl + "/api/media/" + coverImage;
        try {
            log.debug("Fetching media URL for coverImage {} from {}", coverImage, url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("url")) {
                String result = (String) response.get("url");
                mediaUrlCache.put(coverImage, new CachedValue(result));
                return result;
            }
            log.warn("Media response for ID {} did not contain a URL. Response: {}", coverImage, response);
        } catch (Exception e) {
            log.error("Failed to fetch media URL for coverImage {} at {}: {}", coverImage, url, e.getMessage());
        }
        // Cache null/failed result too — prevents repeated slow failing calls
        mediaUrlCache.put(coverImage, new CachedValue(null));
        return null;
    }

    /**
     * Lấy URL ảnh đầu tiên của chiến dịch làm ảnh bìa dự phòng.
     * Cached for 60s.
     */
    public String getFirstImageByCampaignId(Long campaignId) {
        if (campaignId == null) return null;

        CachedValue cached = campaignImageCache.get(campaignId);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }

        String url = mediaServiceUrl + "/api/media/campaigns/" + campaignId + "/first-image";
        try {
            log.debug("Fetching fallback cover image for campaign {} from {}", campaignId, url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("url")) {
                String result = (String) response.get("url");
                campaignImageCache.put(campaignId, new CachedValue(result));
                return result;
            }
        } catch (Exception e) {
            log.warn("No fallback cover image found for campaign {}: {}", campaignId, e.getMessage());
        }
        campaignImageCache.put(campaignId, new CachedValue(null));
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
