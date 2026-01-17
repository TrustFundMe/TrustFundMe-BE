package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountStatusRequest;
import com.trustfund.model.response.BankAccountResponse;

import java.util.List;

public interface BankAccountService {
    BankAccountResponse create(CreateBankAccountRequest request, String currentEmail);

    List<BankAccountResponse> getMyBankAccounts(Long userId);

    BankAccountResponse updateStatus(Long bankAccountId, UpdateBankAccountStatusRequest request, Long currentUserId, String currentRole);

    List<BankAccountResponse> getAllBankAccounts();
}
