package com.trustfund.repository;

import com.trustfund.model.ExpenditureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureItemRepository extends JpaRepository<ExpenditureItem, Long> {
    List<ExpenditureItem> findByExpenditureId(Long expenditureId);

    @org.springframework.data.jpa.repository.Query("SELECT ei FROM ExpenditureItem ei JOIN ei.expenditure e WHERE e.campaignId = :campaignId")
    List<ExpenditureItem> findByExpenditureCampaignId(
            @org.springframework.data.repository.query.Param("campaignId") Long campaignId);

    /**
     * Cố gắng giữ chỗ sản phẩm cuối cùng.
     * Chỉ set reservations=1 khi: reservations=0 VÀ quantity_left - qty = 0
     * Trả về số dòng bị ảnh hưởng: 1 = thành công, 0 = thất bại (đã có người giữ)
     */
    @Modifying
    @Query(value = "UPDATE expenditure_items " +
            "SET reservations = 1 " +
            "WHERE id = :itemId " +
            "  AND reservations = 0 " +
            "  AND quantity_left - :qty = 0", nativeQuery = true)
    int tryReserveLastItem(@Param("itemId") Long itemId, @Param("qty") Integer qty);

    /**
     * Nhả chỗ giữ sản phẩm. Chỉ nhả khi reservations = 1.
     * Trả về số dòng bị ảnh hưởng: 1 = thành công, 0 = không có gì để nhả
     */
    @Modifying
    @Query(value = "UPDATE expenditure_items " +
            "SET reservations = 0 " +
            "WHERE id = :itemId " +
            "  AND reservations = 1", nativeQuery = true)
    int releaseReservation(@Param("itemId") Long itemId);

}
