package com.trustfund.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustfund.entity.SystemConfig;
import com.trustfund.model.response.AuditResultResponse;
import com.trustfund.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerplexityClient {

    @Value("${perplexity.api.key:}") // Fallback in case env not loaded properly, but injected in .env
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

    public AuditResultResponse auditExpenseItems(List<Map<String, Object>> itemsToAudit) {
        String url = "https://api.perplexity.ai/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Fallback reading from System environment variable if spring env parsing
        // missed logic
        if (apiKey == null || apiKey.isEmpty())
            apiKey = System.getenv("PERPLEXITY_API_KEY");
        headers.set("Authorization", "Bearer " + apiKey);

        // Fetch dynamic configuration
        SystemConfig config = systemConfigService.getConfigByKey("ai_audit_prompt");
        String promptInstruction = (config != null && config.getConfigValue() != null)
                ? config.getConfigValue()
                : "You are an independent expense auditor. Analyze the following items, compare their declaredPrice to current market prices, and return a structured JSON.";

        Map<String, Object> schema = null;
        if (config != null && config.getMetadata() != null) {
            try {
                schema = objectMapper.readValue(config.getMetadata(), Map.class);
            } catch (Exception e) {
                log.error("Failed to parse metadata schema", e);
            }
        }

        if (schema == null) {
            // Default Schema Fallback if DB empty or absent
            schema = Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "schema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "items", Map.of(
                                                    "type", "array",
                                                    "items", Map.of(
                                                            "type", "object",
                                                            "properties", Map.of(
                                                                    "itemName", Map.of("type", "string"),
                                                                    "declaredPrice", Map.of("type", "number"),
                                                                    "marketPrice", Map.of("type", "number"),
                                                                    "priceDifferencePercentage",
                                                                    Map.of("type", "number"),
                                                                    "isSuspicious", Map.of("type", "boolean"),
                                                                    "quantity", Map.of("type", "integer"),
                                                                    "statusMessage", Map.of("type", "string"),
                                                                    "evidenceUrls", Map.of(
                                                                            "type", "array",
                                                                            "items", Map.of("type", "string"))),
                                                            "required",
                                                            List.of("itemName", "declaredPrice", "marketPrice",
                                                                    "isSuspicious", "evidenceUrls", "statusMessage"))),
                                            "summary", Map.of("type", "string"),
                                            "requiresAttention", Map.of("type", "boolean")),
                                    "required", List.of("items", "summary", "requiresAttention"))));
        }

        try {
            String content = objectMapper.writeValueAsString(itemsToAudit);
            Map<String, Object> payload = Map.of(
                    "model", "sonar-pro",
                    "messages", List.of(
                            Map.of("role", "system", "content", promptInstruction),
                            Map.of("role", "user", "content",
                                    "Please verify the following expenditure items and find current market prices: "
                                            + content)),
                    "response_format", schema);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String responseContent = (String) message.get("content");
                    return objectMapper.readValue(responseContent, AuditResultResponse.class);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query Perplexity AI", e);
        }
        return null; // Return null if audit fails, will be handled by service
    }
}
