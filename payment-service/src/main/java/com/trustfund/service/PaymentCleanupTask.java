package com.trustfund.service;

import com.trustfund.model.Donation;
import com.trustfund.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCleanupTask {

    private final DonationRepository donationRepository;
    private final DonationService donationService;

    /**
     * Every 1 minute, check for PENDING donations older than 10 minutes
     * and mark them as FAILED.
     */
    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void cleanupPendingTransactions() {
        log.info("⏰ [Cron] Starting Pending Transactions Cleanup...");

        // Threshold: 10 minutes (to avoid failing active user sessions)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        // Cleanup Donations (update status and rollback quantity)
        List<Donation> staleDonations = donationRepository.findAllByStatusAndCreatedAtBefore("PENDING", threshold);
        if (!staleDonations.isEmpty()) {
            log.info("🛠 [Cron] Marking {} stale Donations as FAILED and rolling back quantities",
                    staleDonations.size());
            staleDonations.forEach(donationService::failDonation);
        } else {
            log.debug("✨ [Cron] No stale donations found.");
        }
    }
}
