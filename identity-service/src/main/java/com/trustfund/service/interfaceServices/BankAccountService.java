package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.response.BankAccountResponse;

public interface BankAccountService {
    BankAccountResponse create(CreateBankAccountRequest request, String currentEmail);
}
