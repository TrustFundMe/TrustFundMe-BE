package com.trustfund.repository;

import com.trustfund.model.Module;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByModuleGroupIdOrderByDisplayOrderAsc(Long moduleGroupId);

    boolean existsByModuleGroupIdAndTitle(Long moduleGroupId, String title);

    boolean existsByModuleGroupIdAndUrl(Long moduleGroupId, String url);

    boolean existsByUrl(String url);

    boolean existsByUrlAndIdNot(String url, Long id);

    List<Module> findByIsActive(Boolean isActive);

    @Query("""
    SELECT m FROM Module m
    WHERE
        (
            :keyword IS NULL OR
            LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(m.url) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    AND (:moduleGroupId IS NULL OR m.moduleGroup.id = :moduleGroupId)
    AND (:isActive IS NULL OR m.isActive = :isActive)
""")
    Page<Module> search(
            @Param("keyword") String keyword,
            @Param("moduleGroupId") Long moduleGroupId,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
