package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.BankAccount;
import com.trustfund.model.User;
import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountStatusRequest;
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

    @Override
    public BankAccountResponse updateStatus(Long bankAccountId, UpdateBankAccountStatusRequest request, Long currentUserId, String currentRole) {
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));

        if (bankAccount.getUser() == null || bankAccount.getUser().getId() == null) {
            throw new NotFoundException("Bank account user not found");
        }

        boolean isOwner = bankAccount.getUser().getId().equals(currentUserId);
        String role = currentRole;
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }

        boolean isStaff = role != null && role.equals("STAFF");
        boolean isAdmin = role != null && role.equals("ADMIN");

        if (!isOwner && !isStaff && !isAdmin) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Not allowed to update this bank account");
        }

        String newStatus = request.getStatus();
        if (newStatus == null) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Status is required");
        }

        if (newStatus.equals("DISABLE")) {
            bankAccount.setStatus("DISABLE");
        } else if (newStatus.equals("ACTIVE")) {
            if (!isStaff && !isAdmin) {
                throw new com.trustfund.exception.exceptions.UnauthorizedException("Only staff can activate bank account");
            }
            bankAccount.setStatus("ACTIVE");

            if (request.getIsVerified() != null) {
                bankAccount.setIsVerified(request.getIsVerified());
            }
        } else {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        }

        BankAccount saved = bankAccountRepository.save(bankAccount);
        return toBankAccountResponse(saved);
    }

    @Override
    public List<BankAccountResponse> getAllBankAccounts() {
        return bankAccountRepository.findAll().stream()
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
