package com.trustfund.config;

import com.trustfund.entity.SystemConfig;
import com.trustfund.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class SystemConfigSeeder implements CommandLineRunner {

        private final SystemConfigRepository systemConfigRepository;

        @Override
        public void run(String... args) throws Exception {
                log.info("Checking SystemConfig Seeder...");

                // 1. Market Price Analysis (Perplexity)
                saveConfig("ai_market_analysis_prompt", loadPrompt("ai_market_analysis_prompt.txt"),
                                "Prompt cho Perplexity để check giá thị trường.");

                // 2. Bill Analysis (Vision)
                saveConfig("ai_bill_analysis_prompt", loadPrompt("ai_bill_analysis_prompt.txt"),
                                "Prompt đối soát minh chứng hóa đơn.");

                // 3. Flag Analysis
                saveConfig("ai_flag_analysis_prompt", loadPrompt("ai_flag_analysis_prompt.txt"),
                                "Prompt phân tích báo cáo vi phạm.");

                // 4. OCR Config (JSON)
                saveConfig("ai_ocr_prompt", loadPrompt("ai_ocr_prompt.json"),
                                "Cấu hình prompt OCR mặt trước và mặt sau (JSON).");

                // 5. Campaign Description
                saveConfig("ai_campaign_description_prompt", loadPrompt("ai_campaign_description_prompt.txt"),
                                "Prompt tạo mô tả chiến dịch.");
        }

        private String loadPrompt(String filename) {
                try {
                        ClassPathResource resource = new ClassPathResource("prompts/" + filename);
                        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                        log.error("Failed to load prompt from resource: {}", filename, e);
                        return "";
                }
        }

        private void saveConfig(String key, String value, String description) {
                if (value == null || value.isEmpty())
                        return;

                if (systemConfigRepository.findByConfigKey(key).isEmpty()) {
                        SystemConfig config = SystemConfig.builder()
                                        .configKey(key)
                                        .configValue(value)
                                        .configGroup("AI")
                                        .description(description)
                                        .updatedBy("SYSTEM_SEEDER")
                                        .build();
                        systemConfigRepository.save(config);
                        log.info("Seeded Initial SystemConfig: {}", key);
                } else {
                        log.info("Config already exists, skipping seed for: {}", key);
                }
        }
}
