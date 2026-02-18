package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountStatusRequest;
import com.trustfund.model.response.BankAccountResponse;

import java.util.List;

public interface BankAccountService {
    BankAccountResponse create(CreateBankAccountRequest request, String userIdStr);

    List<BankAccountResponse> getMyBankAccounts(Long userId);

    BankAccountResponse updateStatus(Long bankAccountId, UpdateBankAccountStatusRequest request, Long currentUserId,
            String currentRole);

    List<BankAccountResponse> getAllBankAccounts();

    BankAccountResponse submitBankAccount(Long userId, CreateBankAccountRequest request);

    org.springframework.data.domain.Page<BankAccountResponse> getPendingBankAccounts(
            org.springframework.data.domain.Pageable pageable);

    BankAccountResponse getById(Long id, Long currentUserId, String currentRole);

    BankAccountResponse update(Long id, UpdateBankAccountRequest request, Long currentUserId, String currentRole);

    void delete(Long id, Long currentUserId, String currentRole);

    List<BankAccountResponse> getByUserId(Long userId);
}
