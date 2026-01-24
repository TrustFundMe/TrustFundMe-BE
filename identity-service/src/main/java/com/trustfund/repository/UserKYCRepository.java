package com.trustfund.repository;

import com.trustfund.model.UserKYC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserKYCRepository extends JpaRepository<UserKYC, Long> {
    Optional<UserKYC> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
