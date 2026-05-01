package com.trustfund.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustfund.entity.SystemConfig;
import com.trustfund.model.response.AuditResultResponse;
import com.trustfund.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerplexityClient {

    @Value("${perplexity.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

    private static final String CONFIG_KEY = "ai_market_analysis_prompt";
    private static final String PROMPT_FILE = "prompts/ai_market_analysis_prompt.txt";
    private static final String SCHEMA_FILE = "prompts/ai_market_analysis_schema.json";

    public AuditResultResponse auditExpenseItems(List<Map<String, Object>> itemsToAudit) {
        String url = "https://api.perplexity.ai/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey == null || apiKey.isEmpty())
            apiKey = System.getenv("PERPLEXITY_API_KEY");
        headers.set("Authorization", "Bearer " + apiKey);

        // ── 1. Lấy prompt từ DB, fallback về file nếu DB chưa seed ──────────
        SystemConfig config = systemConfigService.getConfigByKey(CONFIG_KEY);

        String promptInstruction = null;
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
            promptInstruction = config.getConfigValue();
            log.debug("Loaded prompt from DB key: {}", CONFIG_KEY);
        }
        if (promptInstruction == null) {
            promptInstruction = loadFile(PROMPT_FILE);
            log.warn("DB prompt empty — loaded from file: {}", PROMPT_FILE);
        }
        if (promptInstruction == null) {
            promptInstruction = "You are an independent expense auditor. Analyze items and return structured JSON.";
            log.error("Both DB and file prompt unavailable — using minimal fallback");
        }

        // ── 2. Lấy response_format schema từ DB metadata, fallback về file ──
        Map<String, Object> schema = null;
        String schemaJson = (config != null && config.getMetadata() != null && !config.getMetadata().isBlank())
                ? config.getMetadata()
                : loadFile(SCHEMA_FILE);

        if (schemaJson != null) {
            try {
                schema = objectMapper.readValue(schemaJson, Map.class);
                log.debug("Loaded response_format schema successfully");
            } catch (Exception e) {
                log.error("Failed to parse schema JSON — will call without response_format", e);
            }
        } else {
            log.warn("No schema found in DB or file — calling Perplexity without response_format");
        }

        // ── 3. Gọi Perplexity API ─────────────────────────────────────────────
        try {
            String content = objectMapper.writeValueAsString(itemsToAudit);

            Map<String, Object> payload;
            if (schema != null) {
                payload = Map.of(
                        "model", "sonar-pro",
                        "messages", List.of(
                                Map.of("role", "system", "content", promptInstruction),
                                Map.of("role", "user", "content",
                                        "Verify the following expenditure items against current market prices: "
                                                + content)),
                        "response_format", schema);
            } else {
                payload = Map.of(
                        "model", "sonar-pro",
                        "messages", List.of(
                                Map.of("role", "system", "content", promptInstruction),
                                Map.of("role", "user", "content",
                                        "Verify the following expenditure items against current market prices: "
                                                + content)));
            }

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
        return null;
    }

    /** Load một file từ classpath, trả null nếu không tìm thấy */
    private String loadFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Could not load classpath resource: {}", path);
            return null;
        }
    }
}
