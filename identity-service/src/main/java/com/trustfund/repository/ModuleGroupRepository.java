package com.trustfund.repository;

import com.trustfund.model.ModuleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleGroupRepository extends JpaRepository<ModuleGroup, Long> {
    List<ModuleGroup> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
}
