package com.trustfund.repository;

import com.trustfund.model.Flag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlagRepository extends JpaRepository<Flag, Long> {
    Page<Flag> findByStatus(String status, Pageable pageable);

    Page<Flag> findByPostId(Long postId, Pageable pageable);
}
