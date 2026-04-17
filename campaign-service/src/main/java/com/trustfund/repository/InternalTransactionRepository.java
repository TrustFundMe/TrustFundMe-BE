package com.trustfund.repository;

import com.trustfund.model.InternalTransaction;
import com.trustfund.model.enums.InternalTransactionStatus;
import com.trustfund.model.enums.InternalTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InternalTransactionRepository extends JpaRepository<InternalTransaction, Long> {

        List<InternalTransaction> findAllByOrderByCreatedAtDesc();

        List<InternalTransaction> findByFromCampaignIdOrToCampaignIdOrderByCreatedAtDesc(Long fromId, Long toId);

        Page<InternalTransaction> findByFromCampaignIdOrToCampaignIdOrderByCreatedAtDesc(Long fromId, Long toId,
                        Pageable pageable);

        List<InternalTransaction> findByToCampaignIdAndStatusOrderByCreatedAtDesc(
                        Long toCampaignId, InternalTransactionStatus status);

        @Query("SELECT SUM(t.amount) FROM InternalTransaction t WHERE t.fromCampaignId = :campaignId AND t.type = :type AND t.status = :status")
        BigDecimal sumAmountByFromCampaignIdAndTypeAndStatus(
                        @Param("campaignId") Long campaignId,
                        @Param("type") InternalTransactionType type,
                        @Param("status") InternalTransactionStatus status);

        @Query("SELECT SUM(t.amount) FROM InternalTransaction t WHERE t.toCampaignId = :campaignId AND t.type = :type AND t.status = :status")
        BigDecimal sumAmountByToCampaignIdAndTypeAndStatus(
                        @Param("campaignId") Long campaignId,
                        @Param("type") InternalTransactionType type,
                        @Param("status") InternalTransactionStatus status);
}
