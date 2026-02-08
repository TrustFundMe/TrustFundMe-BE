package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.exception.exceptions.UnauthorizedException;
import com.trustfund.model.BankAccount;
import com.trustfund.model.User;
import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountRequest;
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
    public BankAccountResponse create(CreateBankAccountRequest request, String userIdStr) {
        User user = userRepository.findById(Long.parseLong(userIdStr))
                .orElseThrow(() -> new NotFoundException("User not found"));

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
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }

        List<BankAccount> accounts = bankAccountRepository.findByUser_Id(userId);

        return accounts.stream()
                .map(this::toBankAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BankAccountResponse updateStatus(Long bankAccountId, UpdateBankAccountStatusRequest request,
            Long currentUserId, String currentRole) {
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));

        checkBankPermission(bankAccount, currentUserId, currentRole, true);

        String newStatus = request.getStatus();
        if (newStatus == null) {
            throw new BadRequestException("Status is required");
        }

        if (newStatus.equals("DISABLE")) {
            bankAccount.setStatus("DISABLE");
        } else if (newStatus.equals("ACTIVE")) {
            String role = normalizeRole(currentRole);
            if (!"STAFF".equals(role) && !"ADMIN".equals(role)) {
                throw new UnauthorizedException("Only staff or admin can activate bank account");
            }
            bankAccount.setStatus("ACTIVE");

            if (request.getIsVerified() != null) {
                bankAccount.setIsVerified(request.getIsVerified());
            }
        } else {
            throw new BadRequestException("Invalid status: only ACTIVE or DISABLE supported here");
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

    @Override
    public BankAccountResponse getById(Long id, Long currentUserId, String currentRole) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));

        checkBankPermission(bankAccount, currentUserId, currentRole, false);

        return toBankAccountResponse(bankAccount);
    }

    @Override
    public BankAccountResponse update(Long id, UpdateBankAccountRequest request, Long currentUserId,
            String currentRole) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));

        checkBankPermission(bankAccount, currentUserId, currentRole, false);

        bankAccount.setBankCode(request.getBankCode());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setAccountHolderName(request.getAccountHolderName());

        // Reset verification status if details are updated by owner
        if (bankAccount.getUser().getId().equals(currentUserId)) {
            bankAccount.setIsVerified(false);
            bankAccount.setStatus("PENDING");
        }

        BankAccount saved = bankAccountRepository.save(bankAccount);
        return toBankAccountResponse(saved);
    }

    @Override
    public void delete(Long id, Long currentUserId, String currentRole) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));

        checkBankPermission(bankAccount, currentUserId, currentRole, false);

        bankAccountRepository.delete(bankAccount);
    }

    @Override
    public List<BankAccountResponse> getByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        return bankAccountRepository.findByUser_Id(userId).stream()
                .map(this::toBankAccountResponse)
                .collect(Collectors.toList());
    }

    private void checkBankPermission(BankAccount bankAccount, Long currentUserId, String currentRole,
            boolean statusUpdate) {
        if (bankAccount.getUser() == null || bankAccount.getUser().getId() == null) {
            throw new NotFoundException("Bank account user not found");
        }

        boolean isOwner = bankAccount.getUser().getId().equals(currentUserId);
        String role = normalizeRole(currentRole);
        boolean isStaff = "STAFF".equals(role);
        boolean isAdmin = "ADMIN".equals(role);

        if (statusUpdate) {
            // Owner can only DISABLE, Staff/Admin can do anything (checked in updateStatus
            // method)
            if (!isOwner && !isStaff && !isAdmin) {
                throw new UnauthorizedException("Not allowed to update this bank account status");
            }
        } else {
            // For general access/update/delete: owner, staff, or admin
            if (!isOwner && !isStaff && !isAdmin) {
                throw new UnauthorizedException("Not allowed to access/modify this bank account");
            }
        }
    }

    private String normalizeRole(String role) {
        if (role != null && role.startsWith("ROLE_")) {
            return role.substring("ROLE_".length());
        }
        return role;
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
