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
import com.trustfund.repository.UserKYCRepository;
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
    private final UserKYCRepository userKYCRepository;

    @Override
    public BankAccountResponse create(CreateBankAccountRequest request, String userIdStr) {
        User user = userRepository.findById(Long.parseLong(userIdStr))
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (bankAccountRepository.existsByAccountNumberAndBankCodeAndUserIdNot(
                request.getAccountNumber(), request.getBankCode(), user.getId())) {
            throw new BadRequestException("Bank account already exists for another user");
        }

        BankAccount bankAccount = BankAccount.builder()
                .user(user)
                .bankCode(request.getBankCode())
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .isVerified(true) // Auto-verify when STAFF creates
                .status("APPROVED") // Auto-approve when STAFF creates
                .build();

        BankAccount saved = bankAccountRepository.save(bankAccount);

        // Check if should promote user to FUND_OWNER (if KYC is also verified)
        if (shouldPromoteToFundOwner(user.getId())) {
            user.setRole(User.Role.FUND_OWNER);
            userRepository.save(user);
        }

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
        } else if (newStatus.equals("ACTIVE") || newStatus.equals("APPROVED")) {
            String role = normalizeRole(currentRole);
            if (!"STAFF".equals(role) && !"ADMIN".equals(role)) {
                throw new UnauthorizedException("Only staff or admin can activate bank account");
            }
            bankAccount.setStatus("APPROVED");
            bankAccount.setIsVerified(true);

            // Check if user should be promoted to FUND_OWNER
            User user = bankAccount.getUser();
            if (shouldPromoteToFundOwner(user.getId())) {
                user.setRole(User.Role.FUND_OWNER);
                userRepository.save(user);
            }
        } else if (newStatus.equals("REJECTED")) {
            String role = normalizeRole(currentRole);
            if (!"STAFF".equals(role) && !"ADMIN".equals(role)) {
                throw new UnauthorizedException("Only staff or admin can reject bank account");
            }
            bankAccount.setStatus("REJECTED");
            bankAccount.setIsVerified(false);
        } else {
            throw new BadRequestException("Invalid status: only ACTIVE, DISABLE or REJECTED supported here");
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

    /**
     * Check if user should be promoted to FUND_OWNER role.
     * User is promoted when BOTH KYC and Bank Account are verified.
     */
    private boolean shouldPromoteToFundOwner(Long userId) {
        // Check if user has approved KYC
        return userKYCRepository.findByUserId(userId)
                .map(kyc -> kyc.getStatus() == com.trustfund.model.enums.KYCStatus.APPROVED)
                .orElse(false);
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

    @Override
    public BankAccountResponse submitBankAccount(Long userId, CreateBankAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Check if account number + bank code exists for ANY other user
        if (bankAccountRepository.existsByAccountNumberAndBankCodeAndUserIdNot(
                request.getAccountNumber(), request.getBankCode(), userId)) {
            throw new BadRequestException("Bank account already exists for another user");
        }

        // Check if user already has a bank account
        List<BankAccount> existingAccounts = bankAccountRepository.findByUser_Id(userId);
        BankAccount bankAccount;

        if (!existingAccounts.isEmpty()) {
            // Update existing account (take the first one)
            bankAccount = existingAccounts.get(0);
            bankAccount.setBankCode(request.getBankCode());
            bankAccount.setAccountNumber(request.getAccountNumber());
            bankAccount.setAccountHolderName(request.getAccountHolderName());
            bankAccount.setIsVerified(true);
            bankAccount.setStatus("APPROVED");
        } else {
            // Create new account
            bankAccount = BankAccount.builder()
                    .user(user)
                    .bankCode(request.getBankCode())
                    .accountNumber(request.getAccountNumber())
                    .accountHolderName(request.getAccountHolderName())
                    .isVerified(true) // Auto-verify when STAFF submits
                    .status("APPROVED") // Auto-approve when STAFF submits
                    .build();
        }

        BankAccount saved = bankAccountRepository.save(bankAccount);

        // Check if should promote user to FUND_OWNER (if KYC is also verified)
        if (shouldPromoteToFundOwner(user.getId())) {
            user.setRole(User.Role.FUND_OWNER);
            userRepository.save(user);
        }

        return toBankAccountResponse(saved);
    }

    @Override
    public org.springframework.data.domain.Page<BankAccountResponse> getPendingBankAccounts(
            org.springframework.data.domain.Pageable pageable) {
        return bankAccountRepository.findByStatus("PENDING", pageable)
                .map(this::toBankAccountResponse);
    }
}
