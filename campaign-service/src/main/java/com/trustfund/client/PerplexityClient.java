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
        log.info("Starting parallel audit for {} items", itemsToAudit.size());
        
        // Pre-fill list to maintain order of items
        List<com.trustfund.model.response.AuditResultResponse.AuditItem> allDetected = java.util.Collections.synchronizedList(
                new java.util.ArrayList<>(java.util.Collections.nCopies(itemsToAudit.size(), null)));

        java.util.stream.IntStream.range(0, itemsToAudit.size()).parallel().forEach(i -> {
            Map<String, Object> item = itemsToAudit.get(i);
            try {
                AuditResultResponse singleRes = auditSingleExpenseItem(item);
                if (singleRes != null && singleRes.getDetectedItems() != null && !singleRes.getDetectedItems().isEmpty()) {
                    allDetected.set(i, singleRes.getDetectedItems().get(0));
                } else {
                    com.trustfund.model.response.AuditResultResponse.AuditItem emptyItem = new com.trustfund.model.response.AuditResultResponse.AuditItem();
                    String name = item.containsKey("itemName") ? String.valueOf(item.get("itemName")) : String.valueOf(item.get("name"));
                    emptyItem.setName(name);
                    emptyItem.setPriceStatus("UNKNOWN");
                    emptyItem.setStatusMessage("AI không thể phân tích hạng mục này (Không có kết quả).");
                    allDetected.set(i, emptyItem);
                }
            } catch (Exception e) {
                log.error("Error auditing item {}: {}", i, e.getMessage());
                com.trustfund.model.response.AuditResultResponse.AuditItem emptyItem = new com.trustfund.model.response.AuditResultResponse.AuditItem();
                String name = item.containsKey("itemName") ? String.valueOf(item.get("itemName")) : String.valueOf(item.get("name"));
                emptyItem.setName(name);
                emptyItem.setPriceStatus("UNKNOWN");
                emptyItem.setStatusMessage("Lỗi hệ thống khi gọi AI.");
                allDetected.set(i, emptyItem);
            }
        });

        AuditResultResponse combined = new AuditResultResponse();
        combined.setSummary("Tổng hợp kiểm toán AI cho chiến dịch.");
        combined.setRecommendation("Xem chi tiết ở từng hạng mục bên dưới.");
        combined.setRiskScore(0.0);
        combined.setRiskLevel("UNKNOWN");
        combined.setDetectedItems(new java.util.ArrayList<>(allDetected));
        
        return combined;
    }

    public AuditResultResponse auditSingleExpenseItem(Map<String, Object> item) {
        String url = "https://api.perplexity.ai/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey == null || apiKey.isEmpty())
            apiKey = System.getenv("PERPLEXITY_API_KEY");
        headers.set("Authorization", "Bearer " + apiKey);

        // ── Lấy schema từ file (giống auditExpenseItems) ──
        SystemConfig config = systemConfigService.getConfigByKey(CONFIG_KEY);
        Map<String, Object> schema = null;
        String schemaJson = (config != null && config.getMetadata() != null && !config.getMetadata().isBlank())
                ? config.getMetadata()
                : loadFile(SCHEMA_FILE);

        if (schemaJson != null) {
            try {
                schema = objectMapper.readValue(schemaJson,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
            } catch (Exception e) {
            }
        }

        String itemName = item.containsKey("name") ? String.valueOf(item.get("name")) : String.valueOf(item.get("itemName"));
        String brand = item.containsKey("expectedBrand") ? String.valueOf(item.get("expectedBrand")) : String.valueOf(item.get("brand"));
        String unit = item.containsKey("expectedUnit") ? String.valueOf(item.get("expectedUnit")) : String.valueOf(item.get("unit"));
        
        if ("null".equals(itemName)) itemName = "Sản phẩm";
        if ("null".equals(brand)) brand = "Không xác định";
        if ("null".equals(unit)) unit = "Cái/Hộp";

        // ===== BƯỚC 1: Càn quét Link =====
        String step1Prompt = String.format(
            "NHIỆM VỤ: Tìm kiếm danh sách các đường dẫn (URL) bán lẻ trực tuyến cho sản phẩm sau:\n" +
            "- Tên sản phẩm: %s\n" +
            "- Thương hiệu: %s\n" +
            "- Quy cách/Đơn vị: %s\n\n" +
            "YÊU CẦU TÌM KIẾM:\n" +
            "1. Sử dụng từ khóa tìm kiếm: \"%s %s %s\"\n" +
            "2. Tìm 15-20 đường dẫn trực tiếp (direct URLs) từ Shopee Mall, LazMall, Tiki, BHX, Winmart, Coopmart, Lotte.\n" +
            "3. Chỉ lấy link bán đúng sản phẩm, đúng thương hiệu và đúng đơn vị tính.\n" +
            "4. Loại bỏ các trang tin tức, bài viết review, hoặc các trang phân tích dữ liệu JSON.\n" +
            "5. Liệt kê danh sách link kèm giá hiển thị sơ bộ.",
            itemName, brand, unit, itemName, brand, unit
        );

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "You are an expert market analyst. Today is " + LocalDate.now() + ". Respond in Vietnamese."));
        messages.add(Map.of("role", "user", "content", step1Prompt));

        try {
            log.info("[Perplexity] Step 1: Searching links for {}", itemName);
            Map<String, Object> payload1 = new HashMap<>();
            payload1.put("model", "sonar-pro");
            payload1.put("messages", messages);
            payload1.put("temperature", 0.0);

            HttpEntity<Map<String, Object>> entity1 = new HttpEntity<>(payload1, headers);
            ResponseEntity<Map<String, Object>> res1 = restTemplate.postForEntity(url, entity1,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (res1.getStatusCode().is2xxSuccessful() && res1.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) res1.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    String ans1 = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
                    messages.add(Map.of("role", "assistant", "content", ans1));
                }
            }
        } catch (Exception e) {
            log.error("[Perplexity] Step 1 failed", e);
            return null; // Cancel process if step 1 fails
        }

        // ===== BƯỚC 2: Thẩm định & Phân tích =====
        String step2Prompt = String.format(
            "Dựa trên các kết quả tìm kiếm ở trên, hãy thực hiện:\n" +
            "1. Truy cập và xác nhận giá bán thực tế của sản phẩm [%s] thương hiệu [%s] quy cách [%s].\n" +
            "2. Lọc ra ít nhất 4 đường dẫn (URLs) chính xác nhất, uy tín nhất.\n" +
            "3. Tính toán giá thấp nhất (Min), cao nhất (Max) và giá trung bình từ các nguồn này.\n" +
            "LƯU Ý: Tuyệt đối không sử dụng link giả định hoặc link không liên quan đến sản phẩm.",
            itemName, brand, unit
        );
        messages.add(Map.of("role", "user", "content", step2Prompt));

        try {
            log.info("[Perplexity] Step 2: Deep scraping links for {}", itemName);
            Map<String, Object> payload2 = new HashMap<>();
            payload2.put("model", "sonar-pro");
            payload2.put("messages", messages);
            payload2.put("temperature", 0.0);

            HttpEntity<Map<String, Object>> entity2 = new HttpEntity<>(payload2, headers);
            ResponseEntity<Map<String, Object>> res2 = restTemplate.postForEntity(url, entity2,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (res2.getStatusCode().is2xxSuccessful() && res2.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) res2.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    String ans2 = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
                    messages.add(Map.of("role", "assistant", "content", ans2));
                }
            }
        } catch (Exception e) {
            log.error("[Perplexity] Step 2 failed", e);
            return null;
        }

        // ===== BƯỚC 3: Đóng gói JSON =====
        String step3Prompt = "Bây giờ, hãy đóng gói toàn bộ kết quả phân tích ở Bước 2 vào cấu trúc JSON theo response_format. YÊU CẦU QUAN TRỌNG: Mảng 'evidenceUrls' phải chứa các đường dẫn (URLs) thực tế từ các website bán hàng đã tìm thấy ở Bước 1 và Bước 2. Tuyệt đối không đưa link phân tích của AI hoặc link JSON vào mảng này.";
        messages.add(Map.of("role", "user", "content", step3Prompt));

        try {
            log.info("[Perplexity] Step 3: Packaging JSON for {}", itemName);
            Map<String, Object> payload3 = new HashMap<>();
            payload3.put("model", "sonar-pro");
            payload3.put("messages", messages);
            payload3.put("temperature", 0.0);
            if (schema != null) {
                payload3.put("response_format", schema);
            }

            HttpEntity<Map<String, Object>> entity3 = new HttpEntity<>(payload3, headers);
            ResponseEntity<Map<String, Object>> res3 = restTemplate.postForEntity(url, entity3,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (res3.getStatusCode().is2xxSuccessful() && res3.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) res3.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    String jsonAns = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
                    return objectMapper.readValue(jsonAns, AuditResultResponse.class);
                }
            }
        } catch (Exception e) {
            log.error("[Perplexity] Step 3 failed", e);
        }

        return null;
    }

    public AuditResultResponse auditSingleActualExpenseItem(Map<String, Object> item) {
        String url = "https://api.perplexity.ai/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey == null || apiKey.isEmpty())
            apiKey = System.getenv("PERPLEXITY_API_KEY");
        headers.set("Authorization", "Bearer " + apiKey);

        // ── Load Instruction & Schema ──
        SystemConfig config = systemConfigService.getConfigByKey("ai_market_analysis_actual_prompt");
        String promptInstruction = (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank())
                ? config.getConfigValue()
                : loadFile("prompts/ai_market_analysis_actual_prompt.txt");

        if (promptInstruction == null) {
            promptInstruction = "Bạn là một chuyên gia nghiên cứu thị trường. Nhiệm vụ của bạn là thẩm định giá của mặt hàng thực tế đã mua.";
        }

        Map<String, Object> schema = null;
        String schemaJson = (config != null && config.getMetadata() != null && !config.getMetadata().isBlank())
                ? config.getMetadata()
                : loadFile(SCHEMA_FILE);

        if (schemaJson != null) {
            try {
                schema = objectMapper.readValue(schemaJson,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {}
        }

        String itemName = item.containsKey("name") ? String.valueOf(item.get("name")) : String.valueOf(item.get("itemName"));
        String brand = item.containsKey("actualBrand") ? String.valueOf(item.get("actualBrand")) : String.valueOf(item.get("brand"));
        String unit = item.containsKey("actualUnit") ? String.valueOf(item.get("actualUnit")) : String.valueOf(item.get("unit"));
        
        if ("null".equals(itemName)) itemName = "Sản phẩm";
        if ("null".equals(brand)) brand = "Không xác định";
        if ("null".equals(unit)) unit = "Cái/Hộp";

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                promptInstruction + "\nNgày hiện tại: " + LocalDate.now() + ". Toàn bộ phản hồi phải bằng Tiếng Việt."));

        // ===== BƯỚC 1: Tìm kiếm link thực tế =====
        String step1Prompt = String.format(
            "NHIỆM VỤ: Tìm kiếm các đường dẫn (URLs) bán lẻ trực tuyến cho sản phẩm THỰC TẾ sau:\n" +
            "- Tên sản phẩm: %s\n" +
            "- Thương hiệu: %s\n" +
            "- Quy cách/Đơn vị: %s\n\n" +
            "YÊU CẦU TÌM KIẾM:\n" +
            "1. Sử dụng từ khóa tìm kiếm: \"%s %s %s\"\n" +
            "2. Tìm 15-20 đường dẫn trực tiếp từ Shopee Mall, LazMall, Tiki, BHX, Winmart, Coopmart, Lotte.\n" +
            "3. Chỉ lấy link bán đúng sản phẩm và đúng thương hiệu/quy cách.\n" +
            "4. Loại bỏ các trang tin tức, bài viết review, hoặc các link không phải trang bán hàng.\n" +
            "5. Liệt kê danh sách link kèm giá sơ bộ.",
            itemName, brand, unit, itemName, brand, unit
        );

        messages.add(Map.of("role", "user", "content", step1Prompt));

        try {
            log.info("[Perplexity-Actual] Step 1: Searching links for {}", itemName);
            Map<String, Object> payload1 = new HashMap<>();
            payload1.put("model", "sonar-pro");
            payload1.put("messages", messages);
            payload1.put("temperature", 0.0);

            HttpEntity<Map<String, Object>> entity1 = new HttpEntity<>(payload1, headers);
            ResponseEntity<Map<String, Object>> res1 = restTemplate.postForEntity(url, entity1, (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (res1.getStatusCode().is2xxSuccessful() && res1.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) res1.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    String ans1 = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
                    messages.add(Map.of("role", "assistant", "content", ans1));
                }
            }
        } catch (Exception e) {
            log.error("[Perplexity-Actual] Step 1 failed", e);
            return null;
        }

        // ===== BƯỚC 2: Thẩm định giá sâu =====
        String step2Prompt = String.format(
            "Dựa trên danh sách link trên, hãy truy cập vào từng link để xác nhận giá bán chính xác cho sản phẩm [%s] [%s]. " +
            "Sau đó lọc ra ít nhất 4 link tốt nhất (đúng thương hiệu/quy cách nhất) và tính toán giá thấp nhất (Min), cao nhất (Max) và trung bình.",
            brand, unit
        );
        messages.add(Map.of("role", "user", "content", step2Prompt));

        try {
            log.info("[Perplexity-Actual] Step 2: Deep scraping for {}", itemName);
            Map<String, Object> payload2 = new HashMap<>();
            payload2.put("model", "sonar-pro");
            payload2.put("messages", messages);
            payload2.put("temperature", 0.0);

            HttpEntity<Map<String, Object>> entity2 = new HttpEntity<>(payload2, headers);
            ResponseEntity<Map<String, Object>> res2 = restTemplate.postForEntity(url, entity2, (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (res2.getStatusCode().is2xxSuccessful() && res2.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) res2.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    String ans2 = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
                    messages.add(Map.of("role", "assistant", "content", ans2));
                }
            }
        } catch (Exception e) {
            log.error("[Perplexity-Actual] Step 2 failed", e);
            return null;
        }

        // ===== BƯỚC 3: Đóng gói JSON =====
        String step3Prompt = "Bây giờ, hãy đóng gói toàn bộ kết quả phân tích ở Bước 2 vào JSON theo response_format. Mảng 'evidenceUrls' phải chứa các đường dẫn (URLs) bán hàng thực tế đã tìm thấy. Tuyệt đối không được trả về link phân tích JSON của AI.";
        messages.add(Map.of("role", "user", "content", step3Prompt));

        try {
            log.info("[Perplexity-Actual] Step 3: Packaging JSON for {}", itemName);
            Map<String, Object> payload3 = new HashMap<>();
            payload3.put("model", "sonar-pro");
            payload3.put("messages", messages);
            payload3.put("temperature", 0.0);
            if (schema != null) {
                payload3.put("response_format", schema);
            }

            HttpEntity<Map<String, Object>> entity3 = new HttpEntity<>(payload3, headers);
            ResponseEntity<Map<String, Object>> res3 = restTemplate.postForEntity(url, entity3, (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (res3.getStatusCode().is2xxSuccessful() && res3.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) res3.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    String jsonAns = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
                    return objectMapper.readValue(jsonAns, AuditResultResponse.class);
                }
            }
        } catch (Exception e) {
            log.error("[Perplexity-Actual] Step 3 failed", e);
        }

        return null;
    }



    /**
     * Gọi trực tiếp Search API của Perplexity để lấy raw kết quả (Dành cho các case
     * cần dữ liệu thô)
     */
    public List<Map<String, Object>> rawSearch(String query, int maxResults) {
        String url = "https://api.perplexity.ai/search"; // Search endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey == null || apiKey.isEmpty())
            apiKey = System.getenv("PERPLEXITY_API_KEY");
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> payload = Map.of(
                "query", query,
                "max_results", maxResults);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(url, entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("results");
            }
        } catch (Exception e) {
            log.error("[Perplexity] Raw search failed for query: {}", query, e);
        }
        return List.of();
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
