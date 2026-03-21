package com.trustfund.client;

import com.trustfund.model.request.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class NotificationServiceClient {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    public NotificationServiceClient(RestTemplate restTemplate,
            @Value("${notification.service.url:http://localhost:8088}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl.trim().replaceAll("/$", "");
    }

    public void sendNotification(NotificationRequest request) {
        String url = notificationServiceUrl + "/api/notifications/event";
        try {
            log.info("Sending notification event to: {}", url);
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            log.error("Failed to send notification to {}: {}", url, e.getMessage());
            // Do not throw exception to avoid breaking the main identity flow
        }
    }
}
