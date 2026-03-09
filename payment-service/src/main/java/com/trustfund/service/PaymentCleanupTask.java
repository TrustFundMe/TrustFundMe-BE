package com.trustfund.service;

import com.trustfund.model.Donation;
import com.trustfund.model.Payment;
import com.trustfund.repository.DonationRepository;
import com.trustfund.repository.PaymentRepository;
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

    private final PaymentRepository paymentRepository;
    private final DonationRepository donationRepository;

    /**
     * Every 1 minute, check for PENDING payments/donations older than 10 minutes
     * and mark them as FAILED.
     */
    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void cleanupPendingTransactions() {
        log.info("⏰ [Cron] Starting Pending Transactions Cleanup...");

        // Threshold: 10 minutes (to avoid failing active user sessions)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        // 1. Cleanup Payments
        List<Payment> stalePayments = paymentRepository.findAllByStatusAndCreatedAtBefore("PENDING", threshold);
        if (!stalePayments.isEmpty()) {
            log.info("🛠 [Cron] Marking {} stale Payments as FAILED", stalePayments.size());
            stalePayments.forEach(p -> p.setStatus("FAILED"));
            paymentRepository.saveAll(stalePayments);
        }

        // 2. Cleanup Donations
        List<Donation> staleDonations = donationRepository.findAllByStatusAndCreatedAtBefore("PENDING", threshold);
        if (!staleDonations.isEmpty()) {
            log.info("🛠 [Cron] Marking {} stale Donations as FAILED", staleDonations.size());
            staleDonations.forEach(d -> d.setStatus("FAILED"));
            donationRepository.saveAll(staleDonations);
        }

        if (stalePayments.isEmpty() && staleDonations.isEmpty()) {
            log.debug("✨ [Cron] No stale transactions found.");
        }
    }
}
