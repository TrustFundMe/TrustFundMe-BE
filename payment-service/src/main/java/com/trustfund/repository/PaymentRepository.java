package com.trustfund.repository;

import com.trustfund.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentLinkId(String paymentLinkId);

    java.util.List<Payment> findAllByStatusAndCreatedAtBefore(String status, java.time.LocalDateTime threshold);
}
