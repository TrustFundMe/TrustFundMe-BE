package com.trustfund.repository;

import com.trustfund.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonorId(Long donorId);

    List<Donation> findByCampaignId(Long campaignId);

    java.util.Optional<Donation> findByPayment(com.trustfund.model.Payment payment);

    java.util.List<Donation> findAllByStatusAndCreatedAtBefore(String status, java.time.LocalDateTime threshold);
}
