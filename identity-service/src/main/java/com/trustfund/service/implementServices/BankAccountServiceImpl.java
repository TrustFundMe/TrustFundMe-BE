package com.trustfund.service.implementServices;

import com.trustfund.model.BankAccount;
import com.trustfund.model.User;
import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.response.BankAccountResponse;
import com.trustfund.repository.BankAccountRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.interfaceServices.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    public BankAccountResponse create(CreateBankAccountRequest request, String currentEmail) {
        User user = userRepository.findById(Long.parseLong(currentEmail))
                .orElseThrow(() -> new RuntimeException("User not found"));

        BankAccount bankAccount = BankAccount.builder()
                .user(user)
                .bankCode(request.getBankCode())
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .isVerified(false)
                .status("PENDING")
                .build();

        BankAccount saved = bankAccountRepository.save(bankAccount);

        return BankAccountResponse.builder()
                .id(saved.getId())
                .userId(saved.getUser().getId())
                .bankCode(saved.getBankCode())
                .accountNumber(saved.getAccountNumber())
                .accountHolderName(saved.getAccountHolderName())
                .isVerified(saved.getIsVerified())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
