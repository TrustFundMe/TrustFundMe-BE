package com.trustfund.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.trustfund.service.DonationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PusherWebhookListener {

    private final DonationService donationService;
    private final ObjectMapper objectMapper;
    private Pusher pusher;

    @Value("${pusher.key:}")
    private String pusherKey;

    @Value("${pusher.cluster:}")
    private String pusherCluster;

    @PostConstruct
    public void init() {
        if (pusherKey == null || pusherKey.isEmpty() || "null".equals(pusherKey)) {
            log.info("Pusher key is not configured. Webhook forwarder via Pusher is disabled.");
            return;
        }

        try {
            PusherOptions options = new PusherOptions().setCluster(pusherCluster);
            pusher = new Pusher(pusherKey, options);

            pusher.connect(new ConnectionEventListener() {
                @Override
                public void onConnectionStateChange(ConnectionStateChange change) {
                    log.info("Pusher Connection State changed from {} to {}", change.getPreviousState(),
                            change.getCurrentState());
                }

                @Override
                public void onError(String message, String code, Exception e) {
                    log.error("Pusher Connection Error: message={}, code={}", message, code, e);
                }
            }, ConnectionState.ALL);

            Channel channel = pusher.subscribe("payos-webhook");

            channel.bind("payment", new SubscriptionEventListener() {
                @Override
                public void onEvent(PusherEvent event) {
                    log.info("====== PayOS WEBHOOK RECEIVED VIA PUSHER ======");
                    try {
                        String data = event.getData();
                        Map<String, Object> payload;
                        try {
                            // Pusher SDK gives us a JSON string, need to parse it back to Map
                            payload = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
                            });
                        } catch (JsonProcessingException e) {
                            // Sometimes the payload is a nested JSON string, parse twice if needed
                            String unescaped = objectMapper.readValue(data, String.class);
                            payload = objectMapper.readValue(unescaped, new TypeReference<Map<String, Object>>() {
                            });
                        }

                        // Pass to the existing donation service handler
                        donationService.handleWebhook(payload);
                        log.info("Outcome: SUCCESS (via Pusher)");
                    } catch (Exception e) {
                        log.error("Outcome: ERROR (via Pusher) - {}", e.getMessage(), e);
                    }
                }
            });

            log.info("Pusher Webhook Listener initialized and subscribed to 'payos-webhook' channel.");
        } catch (Exception e) {
            log.error("Failed to initialize Pusher: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (pusher != null) {
            pusher.disconnect();
            log.info("Pusher disconnected.");
        }
    }
}
