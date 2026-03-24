package com.trustfund.repository;

import com.trustfund.model.ModuleGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleGroupRepository extends JpaRepository<ModuleGroup, Long> {

    List<ModuleGroup> findAllByOrderByDisplayOrderAsc();

    @Query("""
                SELECT mg FROM ModuleGroup mg
                LEFT JOIN FETCH mg.modules
                ORDER BY mg.displayOrder ASC
            """)
    List<ModuleGroup> findAllWithModules();

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    List<ModuleGroup> findByIsActiveTrueOrderByDisplayOrderAsc();

    @Query("""
                SELECT mg FROM ModuleGroup mg
                WHERE
                    (:keyword IS NULL OR LOWER(mg.name) LIKE :keyword)
                AND (:isActive IS NULL OR mg.isActive = :isActive)
            """)
    Page<ModuleGroup> search(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // ================= LOGIC REORDER =================

    /* CREATE: Chèn vào vị trí X -> Tất cả >= X tăng lên 1 */
    @Modifying
    @Query("UPDATE ModuleGroup mg SET mg.displayOrder = mg.displayOrder + 1 WHERE mg.displayOrder >= :order")
    void shiftOrdersForInsert(@Param("order") Integer order);

    /*
     * UPDATE (Move UP): Chuyển từ dưới lên trên (VD: 5 -> 2)
     * Các phần tử trong khoảng [newOrder, oldOrder) tăng lên 1
     */
    @Modifying
    @Query("UPDATE ModuleGroup mg SET mg.displayOrder = mg.displayOrder + 1 WHERE mg.displayOrder >= :newOrder AND mg.displayOrder < :oldOrder")
    void shiftOrdersForMoveUp(@Param("newOrder") Integer newOrder, @Param("oldOrder") Integer oldOrder);

    /*
     * UPDATE (Move DOWN): Chuyển từ trên xuống dưới (VD: 1 -> 3)
     * Các phần tử trong khoảng (oldOrder, newOrder] giảm đi 1
     */
    @Modifying
    @Query("UPDATE ModuleGroup mg SET mg.displayOrder = mg.displayOrder - 1 WHERE mg.displayOrder > :oldOrder AND mg.displayOrder <= :newOrder")
    void shiftOrdersForMoveDown(@Param("oldOrder") Integer oldOrder, @Param("newOrder") Integer newOrder);
}
