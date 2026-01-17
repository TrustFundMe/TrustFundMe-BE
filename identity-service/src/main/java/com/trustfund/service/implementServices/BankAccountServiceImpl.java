package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.BankAccount;
import com.trustfund.model.User;
import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.response.BankAccountResponse;
import com.trustfund.repository.BankAccountRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.interfaceServices.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

        return toBankAccountResponse(saved);
    }

    @Override
    public List<BankAccountResponse> getMyBankAccounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<BankAccount> accounts = bankAccountRepository.findByUser_Id(userId);
        
        return accounts.stream()
                .map(this::toBankAccountResponse)
                .collect(Collectors.toList());
    }

    private BankAccountResponse toBankAccountResponse(BankAccount bankAccount) {
        return BankAccountResponse.builder()
                .id(bankAccount.getId())
                .userId(bankAccount.getUser().getId())
                .bankCode(bankAccount.getBankCode())
                .accountNumber(bankAccount.getAccountNumber())
                .accountHolderName(bankAccount.getAccountHolderName())
                .isVerified(bankAccount.getIsVerified())
                .status(bankAccount.getStatus())
                .createdAt(bankAccount.getCreatedAt())
                .updatedAt(bankAccount.getUpdatedAt())
                .build();
    }
}
