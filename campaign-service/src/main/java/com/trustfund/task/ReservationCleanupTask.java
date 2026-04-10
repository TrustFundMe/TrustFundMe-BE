package com.trustfund.task;

import com.trustfund.client.PaymentServiceClient;
import com.trustfund.model.ExpenditureItem;
import com.trustfund.repository.ExpenditureItemRepository;
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
public class ReservationCleanupTask {

    private final ExpenditureItemRepository expenditureItemRepository;
    private final PaymentServiceClient paymentServiceClient;

    /**
     * Chạy mỗi phút một lần để kiểm tra các sản phẩm đang bị giữ chỗ quá lâu.
     * Quy tắc:
     * 1. Reservations = 1 và thời gian cập nhật cách đây > 10 phút.
     * 2. Không tìm thấy thông tin payment nào cho ExpenditureItem này bên
     * payment-service.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(10);
        log.info("➔ Starting Reservation Cleanup Task. Checking reservations older than {}", expiryTime);

        // 1. Tìm các items đang bị giữ chỗ (reservations = 1) và đã qua 10 phút
        List<ExpenditureItem> expiredItems = expenditureItemRepository
                .findByReservationsAndUpdatedAtBefore(1, expiryTime);

        if (expiredItems.isEmpty()) {
            return;
        }

        log.info("➔ Found {} potentially expired reservations", expiredItems.size());

        for (ExpenditureItem item : expiredItems) {
            try {
                // 2. Kiểm tra bên payment-service xem đã có payment nào được tạo chưa
                boolean hasPayment = paymentServiceClient.checkPaymentExistsForItem(item.getId());

                if (!hasPayment) {
                    log.info("➔ Item {} has no associated payment after 10 mins. Releasing reservation...",
                            item.getId());
                    // 3. Nếu chưa có payment thì nhả chỗ
                    item.setReservations(0);
                    item.setUpdatedAt(LocalDateTime.now());
                    expenditureItemRepository.save(item);
                    log.info("✅ Released reservation for ExpenditureItem {}", item.getId());
                } else {
                    log.info("➔ Item {} has an associated payment. Keeping reservation active.", item.getId());
                    // Cập nhật lại updatedAt để lần sau đỡ phải check lại ngay lập tức (nếu payment
                    // vẫn đang pending)
                    item.setUpdatedAt(LocalDateTime.now());
                    expenditureItemRepository.save(item);
                }
            } catch (Exception e) {
                log.error("❌ Failed to process cleanup for item {}: {}", item.getId(), e.getMessage());
            }
        }
    }
}
