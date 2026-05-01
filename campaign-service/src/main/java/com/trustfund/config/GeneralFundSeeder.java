package com.trustfund.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GeneralFundSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking and seeding General Fund (ID=1)...");
        try {
            String sql = "INSERT IGNORE INTO campaigns " +
                    "(id, title, fund_owner_id, approved_by_staff, type, status, description, balance, created_at, updated_at) "
                    +
                    "VALUES (1, 'Quỹ Chung', 1, 1, 'GENERAL_FUND', 'APPROVED', 'Quỹ được sử dụng để hỗ trợ và điều phối các chiến dịch cần ngân sách khẩn cấp', 0, NOW(), NOW())";

            int rowsAffected = jdbcTemplate.update(sql);
            if (rowsAffected > 0) {
                log.info("Successfully seeded General Fund with ID=1.");
            } else {
                log.info("General Fund already exists or was skipped.");
            }

            log.info("Seeding system settings...");
            String settingsSql = "INSERT IGNORE INTO system_config " +
                    "(config_key, config_value, description, updated_at) " +
                    "VALUES ('EXPENDITURE_EVIDENCE_DEADLINE_HOURS', '48', 'Thời gian quy định để nộp minh chứng chi tiêu (giờ).', NOW()), " +
                    "('ENFORCEMENT_INTERVAL_MINUTES', '60', 'Tần suất quét và khóa quỹ vi phạm (phút).', NOW())";
            jdbcTemplate.update(settingsSql);
            log.info("Successfully seeded system setting for evidence deadline.");
        } catch (Exception e) {
            log.error(
                    "Failed to seed General Fund. If campaigns table does not exist yet wait for next restart. Error: {}",
                    e.getMessage());
        }
    }
}
