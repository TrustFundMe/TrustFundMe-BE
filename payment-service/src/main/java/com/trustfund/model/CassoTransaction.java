package com.trustfund.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "casso_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CassoTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tid;

    private String accountNumber;
    
    private String bankName;
    
    private String bankAbbreviation;

    private Long campaignId;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal amount;
    
    private String description;
    
    private String transactionDate;
    
    private String counterAccountName;
    private String counterAccountNumber;
    private String counterAccountBankName;
    private String counterAccountBankId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
