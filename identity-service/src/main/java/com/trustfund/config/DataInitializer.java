package com.trustfund.config;

import com.trustfund.model.Module;
import com.trustfund.model.ModuleGroup;
import com.trustfund.repository.ModuleGroupRepository;
import com.trustfund.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ModuleGroupRepository moduleGroupRepository;
    private final ModuleRepository moduleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (moduleGroupRepository.count() == 0) {
            initializeMenu();
        }
    }

    @Transactional
    private void initializeMenu() {
        // === GROUP ===
        ModuleGroup managementGroup = ModuleGroup.builder()
                .name("Quản lý hệ thống")
                .isActive(true)
                .displayOrder(1)
                .build();
        moduleGroupRepository.save(managementGroup);

        // === LEVEL 0: Dashboard (standalone) ===
        Module dashboard = Module.builder()
                .title("Bảng điều khiển")
                .url("/admin")
                .icon("dashboard")
                .displayOrder(0)
                .isActive(true)
                .moduleGroup(managementGroup)
                .build();
        moduleRepository.save(dashboard);

        // === LEVEL 1: Parent menu "Quản lý" (no url, has children) ===
        Module mgmtParent = Module.builder()
                .title("Quản lý")
                .url("")
                .icon("menu")
                .displayOrder(1)
                .isActive(true)
                .requiredPermission("VIEW_USERS")
                .moduleGroup(managementGroup)
                .build();
        moduleRepository.save(mgmtParent);

        // === LEVEL 2: Children of "Quản lý" ===
        Module users = Module.builder()
                .title("Quản lý Người dùng")
                .url("/admin/users")
                .icon("users")
                .displayOrder(0)
                .isActive(true)
                .requiredPermission("VIEW_USERS")
                .moduleGroup(managementGroup)
                .parent(mgmtParent)
                .build();

        Module campaigns = Module.builder()
                .title("Quản lý Chiến dịch")
                .url("/admin/campaigns")
                .icon("folder")
                .displayOrder(1)
                .isActive(true)
                .requiredPermission("VIEW_CAMPAIGNS")
                .moduleGroup(managementGroup)
                .parent(mgmtParent)
                .build();

        Module payouts = Module.builder()
                .title("Quản lý Giải ngân")
                .url("/admin/payouts")
                .icon("clipboard-check")
                .displayOrder(2)
                .isActive(true)
                .requiredPermission("VIEW_PAYOUTS")
                .moduleGroup(managementGroup)
                .parent(mgmtParent)
                .build();

        moduleRepository.saveAll(List.of(users, campaigns, payouts));

        System.out.println("✅ Initialized dynamic menu data with grouped structure!");
    }
}
