package com.trustfund.repository;

import com.trustfund.model.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Donation d WHERE d.id = :id")
    java.util.Optional<Donation> findByIdWithLock(@Param("id") Long id);

    List<Donation> findByDonorId(Long donorId);

    List<Donation> findByCampaignId(Long campaignId);

    java.util.Optional<Donation> findByOrderCode(Long orderCode);

    java.util.List<Donation> findAllByStatusAndCreatedAtBefore(String status, java.time.LocalDateTime threshold);

    @Query("SELECT COALESCE(SUM(d.donationAmount), 0) FROM Donation d WHERE d.campaignId = :campaignId AND d.status = 'PAID'")
    BigDecimal sumDonationAmountByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT d FROM Donation d WHERE d.campaignId = :campaignId AND d.status = 'PAID' ORDER BY d.createdAt DESC")
    List<Donation> findRecentPaidDonationsByCampaignId(@Param("campaignId") Long campaignId,
            org.springframework.data.domain.Pageable pageable);

    List<Donation> findByCampaignIdAndStatusOrderByCreatedAtAsc(Long campaignId, String status);

    List<Donation> findByDonorIdAndStatusOrderByCreatedAtDesc(Long donorId, String status);

    List<Donation> findByStatusOrderByCreatedAtDesc(String status);

    List<Donation> findByCampaignIdAndStatusOrderByCreatedAtDesc(Long campaignId, String status);

    Page<Donation> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT d.donorId) FROM Donation d WHERE d.campaignId = :campaignId AND d.status = 'PAID'")
    Long countUniqueDonorsByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.donorId = :donorId AND d.status = 'PAID'")
    Long countByDonorId(@Param("donorId") Long donorId);

    @Query("SELECT COALESCE(SUM(d.donationAmount), 0) FROM Donation d WHERE d.campaignId IN :campaignIds AND d.status = 'PAID'")
    BigDecimal sumDonationAmountByCampaignIds(@Param("campaignIds") List<Long> campaignIds);
}
