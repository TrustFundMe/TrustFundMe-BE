package com.trustfund.repository;

import com.trustfund.model.TrustScoreConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrustScoreConfigRepository extends JpaRepository<TrustScoreConfig, Long> {

    List<TrustScoreConfig> findAll();

    Optional<TrustScoreConfig> findByRuleKey(String ruleKey);
}
