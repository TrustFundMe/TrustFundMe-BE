package com.trustfund.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustfund.entity.SystemConfig;
import com.trustfund.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class SystemConfigSeeder implements CommandLineRunner {

        private final SystemConfigRepository systemConfigRepository;
        private final ObjectMapper objectMapper;

        @Override
        public void run(String... args) throws Exception {
                log.info("Checking SystemConfig Seeder...");
                SystemConfig existing = systemConfigRepository.findByConfigKey("ai_audit_prompt").orElse(null);

                Map<String, Object> schema = Map.of(
                                "type", "json_schema",
                                "json_schema", Map.of(
                                                "schema", Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "items", Map.of(
                                                                                                "type", "array",
                                                                                                "items", Map.of(
                                                                                                                "type",
                                                                                                                "object",
                                                                                                                "properties",
                                                                                                                Map.of(
                                                                                                                                "itemName",
                                                                                                                                Map.of("type", "string"),
                                                                                                                                "declaredPrice",
                                                                                                                                Map.of("type", "number"),
                                                                                                                                "marketPrice",
                                                                                                                                Map.of("type", "number"),
                                                                                                                                "priceDifferencePercentage",
                                                                                                                                Map.of("type", "number"),
                                                                                                                                "isSuspicious",
                                                                                                                                Map.of("type", "boolean"),
                                                                                                                                "quantity",
                                                                                                                                Map.of("type", "integer"),
                                                                                                                                "statusMessage",
                                                                                                                                Map.of("type", "string"),
                                                                                                                                "evidenceUrls",
                                                                                                                                Map.of(
                                                                                                                                                "type",
                                                                                                                                                "array",
                                                                                                                                                "items",
                                                                                                                                                Map.of("type", "string"))),
                                                                                                                "required",
                                                                                                                List.of("itemName",
                                                                                                                                "declaredPrice",
                                                                                                                                "marketPrice",
                                                                                                                                "isSuspicious",
                                                                                                                                "evidenceUrls",
                                                                                                                                "statusMessage"))),
                                                                                "summary", Map.of("type", "string"),
                                                                                "requiresAttention",
                                                                                Map.of("type", "boolean")),
                                                                "required",
                                                                List.of("items", "summary", "requiresAttention"))));

                String strictPrompt = "Bạn là Kiểm toán viên Độc lập. Hãy phân tích các hàng hóa được gửi lên, đối chiếu với thị trường thực tế tại Việt Nam. YÊU CẦU BẮT BUỘC:\n"
                                + "1. KIỂM TRA SỰ TỒN TẠI (ITEM + BRAND): Trước khi check giá, hãy rà soát xem sự kết hợp giữa 'Tên sản phẩm' và 'Nhãn hiệu' (brand) có tồn tại thực tế không. \n"
                                + "   - Ví dụ: 'Phở gói' + 'SiuKay' -> Nếu không có phở gói SiuKay (chỉ có mì) -> Đánh dấu là KHÔNG TỒN TẠI.\n"
                                + "2. XỬ LÝ THÔNG TIN CHUNG CHUNG (VAGUE INFORMATION): \n"
                                + "   - Nếu tên sản phẩm quá chung chung (Ví dụ: 'Mì', 'Trứng', 'Gạo' mà không rõ loại/nhãn hiệu) dẫn đến khó xác định giá chính xác: Hãy đưa ra giá trung bình thị trường và đặt statusMessage = 'Thiếu thông tin phân loại'.\n"
                                + "3. BẰNG CHỨNG CHO SẢN PHẨM KHÔNG TỒN TẠI: \n"
                                + "   - Nếu sản phẩm KHÔNG tồn tại: Hãy cung cấp evidenceUrls dẫn đến: (a) Trang danh mục sản phẩm chính thức của nhãn hiệu đó để chứng minh không có món này, hoặc (b) Link sản phẩm 'gần đúng' nhất (ví dụ link Mì SiuKay) và giải thích trong statusMessage là 'Sản phẩm không tồn tại'.\n"
                                + "4. TRẠNG THÁI statusMessage: \n"
                                + "   - Nếu sản phẩm/brand KHÔNG tồn tại: đặt isSuspicious = true, marketPrice = 0, và statusMessage = 'Sản phẩm không tồn tại'.\n"
                                + "   - Nếu sản phẩm tồn tại nhưng giá chênh lệch: đặt statusMessage = 'Giá bất thường'.\n"
                                + "   - Nếu thông tin quá chung chung: đặt statusMessage = 'Thiếu thông tin phân loại'.\n"
                                + "   - Nếu mọi thứ ổn: đặt statusMessage = 'Hợp lý'.\n"
                                + "5. Sử dụng thông tin 'unit' và 'purchaseLocation' để đối chiếu giá siêu thị/chợ chính xác.\n"
                                + "6. Tuyệt đối không dùng 'N/A'. Trả về JSON Schema kèm evidenceUrls.";

                if (existing == null) {
                        SystemConfig aiAuditConfig = SystemConfig.builder()
                                        .configKey("ai_audit_prompt")
                                        .configGroup("AI")
                                        .configValue(strictPrompt)
                                        .description("Prompt phân tích chi tiêu bằng Perplexity. Giá trị value là System Instruction, metadata là JSON Schema bắt buộc.")
                                        .metadata(objectMapper.writeValueAsString(schema))
                                        .updatedBy("SYSTEM_SEEDER")
                                        .build();
                        systemConfigRepository.save(aiAuditConfig);
                        log.info("Inserted default SystemConfig: ai_audit_prompt");
                } else {
                        existing.setConfigValue(strictPrompt);
                        existing.setMetadata(objectMapper.writeValueAsString(schema));
                        systemConfigRepository.save(existing);
                        log.info("Updated default SystemConfig: ai_audit_prompt with STRICT brand checking rules");
                }
        }
}
