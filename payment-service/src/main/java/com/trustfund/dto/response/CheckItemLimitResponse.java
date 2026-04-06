package com.trustfund.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckItemLimitResponse {
    private boolean canDonateMore;
    private int quantityLeft;
    private String message;
    private boolean checkSuccessful;
}
