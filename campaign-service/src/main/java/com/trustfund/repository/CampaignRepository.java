package com.trustfund.repository;

import com.trustfund.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

        org.springframework.data.domain.Page<Campaign> findByFundOwnerIdAndTypeNot(Long fundOwnerId, String type,
                        org.springframework.data.domain.Pageable pageable);

        List<Campaign> findByStatusAndTypeNot(String status, String type);

        List<Campaign> findByCategoryIdAndTypeNot(Long categoryId, String type);

        org.springframework.data.domain.Page<Campaign> findByTypeNot(String type,
                        org.springframework.data.domain.Pageable pageable);

        List<Campaign> findByTypeNot(String type, org.springframework.data.domain.Sort sort);

        List<Campaign> findByFundOwnerIdAndTypeNot(Long fundOwnerId, String type);

        long countByFundOwnerId(Long fundOwnerId);

        @org.springframework.data.jpa.repository.Query("SELECT c.id FROM Campaign c WHERE c.fundOwnerId = :fundOwnerId AND c.type <> 'GENERAL_FUND'")
        java.util.List<Long> findIdsByFundOwnerId(
                        @org.springframework.data.repository.query.Param("fundOwnerId") Long fundOwnerId);

        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.balance), 0) FROM Campaign c WHERE c.fundOwnerId = :fundOwnerId AND c.type <> 'GENERAL_FUND'")
        java.math.BigDecimal sumBalanceByFundOwnerId(
                        @org.springframework.data.repository.query.Param("fundOwnerId") Long fundOwnerId);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.data.jpa.repository.Query("UPDATE Campaign c SET c.balance = c.balance + :amount WHERE c.id = :id")
        void updateBalance(@org.springframework.data.repository.query.Param("id") Long id,
                        @org.springframework.data.repository.query.Param("amount") java.math.BigDecimal amount);

        List<Campaign> findByFundOwnerId(Long fundOwnerId);
}
