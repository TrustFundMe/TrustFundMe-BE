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
import java.time.LocalDate;
import java.util.HashMap;
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
            log.info("[Perplexity] Prompt loaded from DB ({} chars): key={}", promptInstruction.length(), CONFIG_KEY);
        }
        if (promptInstruction == null) {
            promptInstruction = loadFile(PROMPT_FILE);
            log.warn("[Perplexity] DB prompt empty — loaded from file: {} ({} chars)", PROMPT_FILE,
                    promptInstruction != null ? promptInstruction.length() : 0);
        }
        if (promptInstruction == null) {
            promptInstruction = "You are an independent expense auditor. Analyze items and return structured JSON.";
            log.error("[Perplexity] Both DB and file prompt unavailable — using minimal fallback");
        }

        // ── 2. Lấy response_format schema từ DB metadata, fallback về file ──
        Map<String, Object> schema = null;
        String schemaJson = (config != null && config.getMetadata() != null && !config.getMetadata().isBlank())
                ? config.getMetadata()
                : loadFile(SCHEMA_FILE);

        if (schemaJson != null) {
            log.info("[Perplexity] Schema source: {} ({} chars)",
                    (config != null && config.getMetadata() != null && !config.getMetadata().isBlank())
                            ? "DB metadata"
                            : "file: " + SCHEMA_FILE,
                    schemaJson.length());
            try {
                schema = objectMapper.readValue(schemaJson, Map.class);
                log.info("[Perplexity] response_format schema parsed OK. Type: {}, name: {}",
                        schema.get("type"),
                        schema.get("json_schema") != null ? ((Map<?, ?>) schema.get("json_schema")).get("name")
                                : "N/A");
            } catch (Exception e) {
                log.error("[Perplexity] Failed to parse schema JSON — will call without response_format", e);
            }
        } else {
            log.warn("[Perplexity] No schema found in DB or file — calling Perplexity WITHOUT response_format");
        }

        // ── 3. Gọi Perplexity API ─────────────────────────────────────────────
        try {
            String content = objectMapper.writeValueAsString(itemsToAudit);
            String dateContext = String.format(
                    "\nHôm nay là ngày %s. Hãy tìm giá thị trường có hiệu lực tại thời điểm này.",
                    LocalDate.now().toString());
            String fullSystemPrompt = promptInstruction + dateContext;

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "sonar-pro");
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", fullSystemPrompt),
                    Map.of("role", "user", "content",
                            "Verify the following expenditure items against current market prices: " + content)));
            payload.put("temperature", 0.0);
            payload.put("top_p", 1.0);

            if (schema != null) {
                payload.put("response_format", schema);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String responseContent = (String) message.get("content");
                    log.info("[Perplexity] Raw response (first 500 chars): {}",
                            responseContent != null && responseContent.length() > 500
                                    ? responseContent.substring(0, 500) + "..."
                                    : responseContent);
                    AuditResultResponse result = objectMapper.readValue(responseContent, AuditResultResponse.class);
                    if (result != null && (result.getDetectedItems() == null || result.getDetectedItems().isEmpty())) {
                        log.warn("[Perplexity] detectedItems is NULL or EMPTY in the parsed response! Raw: {}",
                                responseContent);
                    }
                    return result;
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
