package com.trustfund.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenditure_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenditureTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_id", nullable = false)
    private Expenditure expenditure;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "from_bank_code", length = 50)
    private String fromBankCode;

    @Column(name = "from_account_number", length = 50)
    private String fromAccountNumber;

    @Column(name = "from_account_holder_name", length = 255)
    private String fromAccountHolderName;

    @Column(name = "to_bank_code", length = 50)
    private String toBankCode;

    @Column(name = "to_account_number", length = 50)
    private String toAccountNumber;

    @Column(name = "to_account_holder_name", length = 255)
    private String toAccountHolderName;

    @Column(name = "type", length = 50, nullable = false)
    private String type; // PAYOUT, REFUND

    @Column(name = "proof_url", length = 1000)
    private String proofUrl;

    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
