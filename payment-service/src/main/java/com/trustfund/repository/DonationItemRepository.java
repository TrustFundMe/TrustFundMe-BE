package com.trustfund.repository;

import com.trustfund.model.DonationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonationItemRepository extends JpaRepository<DonationItem, Long> {
    List<DonationItem> findByDonationId(Long donationId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(di.quantity) FROM DonationItem di WHERE di.expenditureItemId = :expenditureItemId AND di.donation.status = 'PAID'")
    Integer sumQuantityByExpenditureItemId(
            @org.springframework.data.repository.query.Param("expenditureItemId") Long expenditureItemId);
}
