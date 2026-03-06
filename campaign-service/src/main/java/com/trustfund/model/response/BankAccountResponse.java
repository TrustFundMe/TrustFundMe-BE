package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountResponse {
    private Long id;
    private Long userId;
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;
    private Boolean isVerified;
    private String status;
}
