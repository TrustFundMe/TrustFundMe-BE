package com.trustfund.repository;

import com.trustfund.model.InternalTransaction;
import com.trustfund.model.enums.InternalTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InternalTransactionRepository extends JpaRepository<InternalTransaction, Long> {

    List<InternalTransaction> findByFromCampaignIdOrToCampaignIdOrderByCreatedAtDesc(Long fromId, Long toId);

    @Query("SELECT SUM(t.amount) FROM InternalTransaction t WHERE t.fromCampaignId = :campaignId AND t.type = :type")
    BigDecimal sumAmountByFromCampaignIdAndType(@Param("campaignId") Long campaignId, @Param("type") InternalTransactionType type);

    @Query("SELECT SUM(t.amount) FROM InternalTransaction t WHERE t.toCampaignId = :campaignId AND t.type = :type")
    BigDecimal sumAmountByToCampaignIdAndType(@Param("campaignId") Long campaignId, @Param("type") InternalTransactionType type);
}
