package com.trustfund.repository;

import com.trustfund.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByEmailAndOtpAndUsedFalse(String email, String otp);
    
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < ?1")
    void deleteExpiredOtp(LocalDateTime now);
    
    @Modifying
    @Query("UPDATE OtpToken o SET o.used = true WHERE o.email = ?1 AND o.otp = ?2")
    void markAsUsed(String email, String otp);
}
