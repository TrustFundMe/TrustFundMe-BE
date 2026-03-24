package com.trustfund.config;

import com.trustfund.model.Module;
import com.trustfund.model.ModuleGroup;
import com.trustfund.repository.ModuleGroupRepository;
import com.trustfund.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
        List<ModuleGroup> groups = new ArrayList<>();
        List<Module> allModules = new ArrayList<>();

        // === GROUP 1: Main Menu ===
        ModuleGroup mainMenu = ModuleGroup.builder()
                .name("Main Menu")
                .description("Main navigation menu")
                .isActive(true)
                .displayOrder(1)
                .build();
        groups.add(moduleGroupRepository.save(mainMenu));

        allModules.add(Module.builder()
                .title("Dashboard")
                .url("/dashboard")
                .icon("home")
                .displayOrder(0)
                .isActive(true)
                .moduleGroup(mainMenu)
                .build());

        // === GROUP 2: User Management ===
        ModuleGroup userMgmt = ModuleGroup.builder()
                .name("User Management")
                .description("Manage users, roles, and access")
                .isActive(true)
                .displayOrder(2)
                .build();
        groups.add(moduleGroupRepository.save(userMgmt));

        allModules.add(Module.builder()
                .title("Users")
                .url("/users")
                .icon("users")
                .displayOrder(0)
                .isActive(true)
                .moduleGroup(userMgmt)
                .build());
        allModules.add(Module.builder()
                .title("Roles")
                .url("/roles")
                .icon("shield")
                .displayOrder(1)
                .isActive(true)
                .moduleGroup(userMgmt)
                .build());
        allModules.add(Module.builder()
                .title("KYC Verification")
                .url("/kyc")
                .icon("user-check")
                .displayOrder(2)
                .isActive(true)
                .moduleGroup(userMgmt)
                .build());

        // === GROUP 3: Campaign Management ===
        ModuleGroup campaignMgmt = ModuleGroup.builder()
                .name("Campaign Management")
                .description("Manage fundraising campaigns")
                .isActive(true)
                .displayOrder(3)
                .build();
        groups.add(moduleGroupRepository.save(campaignMgmt));

        allModules.add(Module.builder()
                .title("Campaigns")
                .url("/campaigns")
                .icon("folder")
                .displayOrder(0)
                .isActive(true)
                .moduleGroup(campaignMgmt)
                .build());
        allModules.add(Module.builder()
                .title("Categories")
                .url("/categories")
                .icon("tag")
                .displayOrder(1)
                .isActive(true)
                .moduleGroup(campaignMgmt)
                .build());
        allModules.add(Module.builder()
                .title("Fundraising Goals")
                .url("/fundraising-goals")
                .icon("target")
                .displayOrder(2)
                .isActive(true)
                .moduleGroup(campaignMgmt)
                .build());
        allModules.add(Module.builder()
                .title("Expenditures")
                .url("/expenditures")
                .icon("credit-card")
                .displayOrder(3)
                .isActive(true)
                .moduleGroup(campaignMgmt)
                .build());

        // === GROUP 4: Donation & Payment ===
        ModuleGroup donationMgmt = ModuleGroup.builder()
                .name("Donation & Payment")
                .description("Manage donations and payments")
                .isActive(true)
                .displayOrder(4)
                .build();
        groups.add(moduleGroupRepository.save(donationMgmt));

        allModules.add(Module.builder()
                .title("Donations")
                .url("/donations")
                .icon("heart")
                .displayOrder(0)
                .isActive(true)
                .moduleGroup(donationMgmt)
                .build());
        allModules.add(Module.builder()
                .title("Payment History")
                .url("/payments")
                .icon("dollar-sign")
                .displayOrder(1)
                .isActive(true)
                .moduleGroup(donationMgmt)
                .build());
        allModules.add(Module.builder()
                .title("Bank Accounts")
                .url("/bank-accounts")
                .icon("building")
                .displayOrder(2)
                .isActive(true)
                .moduleGroup(donationMgmt)
                .build());

        // === GROUP 5: Payout Management ===
        ModuleGroup payoutMgmt = ModuleGroup.builder()
                .name("Payout Management")
                .description("Manage fund disbursement and payouts")
                .isActive(true)
                .displayOrder(5)
                .build();
        groups.add(moduleGroupRepository.save(payoutMgmt));

        allModules.add(Module.builder()
                .title("Payout Requests")
                .url("/payouts")
                .icon("clipboard-check")
                .displayOrder(0)
                .isActive(true)
                .moduleGroup(payoutMgmt)
                .build());
        allModules.add(Module.builder()
                .title("Payout History")
                .url("/payout-history")
                .icon("history")
                .displayOrder(1)
                .isActive(true)
                .moduleGroup(payoutMgmt)
                .build());

        // === GROUP 6: Communication ===
        ModuleGroup communication = ModuleGroup.builder()
                .name("Communication")
                .description("Chat, forum, and announcements")
                .isActive(true)
                .displayOrder(6)
                .build();
        groups.add(moduleGroupRepository.save(communication));

        allModules.add(Module.builder()
                .title("Chat")
                .url("/chat")
                .icon("message-circle")
                .displayOrder(0)
                .isActive(true)
                .moduleGroup(communication)
                .build());
        allModules.add(Module.builder()
                .title("Forum")
                .url("/forum")
                .icon("message-square")
                .displayOrder(1)
                .isActive(true)
                .moduleGroup(communication)
                .build());
        allModules.add(Module.builder()
                .title("Feed")
                .url("/feed")
                .icon("rss")
                .displayOrder(2)
                .isActive(true)
                .moduleGroup(communication)
                .build());
        allModules.add(Module.builder()
                .title("Notifications")
                .url("/notifications")
                .icon("bell")
                .displayOrder(3)
                .isActive(true)
                .moduleGroup(communication)
                .build());

        // === GROUP 7: System Management ===
        ModuleGroup systemMgmt = ModuleGroup.builder()
                .name("System Management")
                .description("System configuration and settings")
                .isActive(true)
                .displayOrder(7)
                .build();
        groups.add(moduleGroupRepository.save(systemMgmt));

        allModules.add(Module.builder()
                .title("Module Groups")
                .url("/module-groups")
                .icon("layers")
                .displayOrder(0)
                .isActive(true)
                .moduleGroup(systemMgmt)
                .build());
        allModules.add(Module.builder()
                .title("Modules")
                .url("/modules")
                .icon("menu")
                .displayOrder(1)
                .isActive(true)
                .moduleGroup(systemMgmt)
                .build());

        moduleRepository.saveAll(allModules);

        System.out.println("✅ Initialized dynamic menu with " + groups.size() + " groups and " + allModules.size() + " modules!");
    }
}
