package com.trustfund.repository;

import com.trustfund.model.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {

    Optional<ForumCategory> findBySlug(String slug);

    List<ForumCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
}
