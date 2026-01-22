package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckEmailResponse {
    private Boolean exists;
    private String email;
    private String fullName; // Optional: return user name if exists
}
