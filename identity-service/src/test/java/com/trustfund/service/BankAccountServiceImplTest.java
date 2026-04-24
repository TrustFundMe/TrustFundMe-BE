package com.trustfund.service;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.exception.exceptions.UnauthorizedException;
import com.trustfund.model.BankAccount;
import com.trustfund.model.User;
import com.trustfund.model.enums.KYCStatus;
import com.trustfund.model.UserKYC;
import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountStatusRequest;
import com.trustfund.model.response.BankAccountResponse;
import com.trustfund.repository.BankAccountRepository;
import com.trustfund.repository.UserKYCRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.implementServices.BankAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private UserKYCRepository userKYCRepository;

    @InjectMocks private BankAccountServiceImpl service;

    private User user;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("u@e.com").role(User.Role.USER).build();
        account = BankAccount.builder()
                .id(10L).user(user).bankCode("VCB").accountNumber("123456")
                .accountHolderName("NGUYEN VAN A").isVerified(false).status("PENDING").build();
    }

    private CreateBankAccountRequest createReq() {
        return CreateBankAccountRequest.builder()
                .bankCode("VCB").accountNumber("123456").accountHolderName("NGUYEN VAN A").build();
    }

    @Test @DisplayName("create_success_returnsApprovedAccount")
    void create_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bankAccountRepository.existsByAccountNumberAndBankCodeAndUserIdNot(any(), any(), any())).thenReturn(false);
        when(bankAccountRepository.save(any())).thenAnswer(i -> { BankAccount b = i.getArgument(0); b.setId(99L); return b; });
        when(userKYCRepository.findByUserId(1L)).thenReturn(Optional.empty());

        BankAccountResponse r = service.create(createReq(), "1");
        assertThat(r.getStatus()).isEqualTo("APPROVED");
        assertThat(r.getIsVerified()).isTrue();
    }

    @Test @DisplayName("create_userNotFound_throws")
    void create_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(createReq(), "1")).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("create_duplicateAccount_throws")
    void create_duplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bankAccountRepository.existsByAccountNumberAndBankCodeAndUserIdNot(any(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.create(createReq(), "1")).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("create_promotesToFundOwner_whenKycApproved")
    void create_promotes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bankAccountRepository.existsByAccountNumberAndBankCodeAndUserIdNot(any(), any(), any())).thenReturn(false);
        when(bankAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        UserKYC kyc = UserKYC.builder().status(KYCStatus.APPROVED).build();
        when(userKYCRepository.findByUserId(1L)).thenReturn(Optional.of(kyc));

        service.create(createReq(), "1");
        assertThat(user.getRole()).isEqualTo(User.Role.FUND_OWNER);
    }

    @Test @DisplayName("getMyBankAccounts_userNotExist_throws")
    void getMy_userNotExist() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.getMyBankAccounts(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("getMyBankAccounts_returnsList")
    void getMy_ok() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(bankAccountRepository.findByUser_Id(1L)).thenReturn(List.of(account));
        List<BankAccountResponse> r = service.getMyBankAccounts(1L);
        assertThat(r).hasSize(1);
    }

    @Test @DisplayName("updateStatus_notFound_throws")
    void updateStatus_notFound() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.empty());
        UpdateBankAccountStatusRequest req = UpdateBankAccountStatusRequest.builder().status("ACTIVE").build();
        assertThatThrownBy(() -> service.updateStatus(10L, req, 1L, "STAFF"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("updateStatus_nullStatus_throwsBadRequest")
    void updateStatus_nullStatus() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        UpdateBankAccountStatusRequest req = UpdateBankAccountStatusRequest.builder().status(null).build();
        assertThatThrownBy(() -> service.updateStatus(10L, req, 1L, "STAFF"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("updateStatus_disable_byOwner_succeeds")
    void updateStatus_disable() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        UpdateBankAccountStatusRequest req = UpdateBankAccountStatusRequest.builder().status("DISABLE").build();
        BankAccountResponse r = service.updateStatus(10L, req, 1L, "USER");
        assertThat(r.getStatus()).isEqualTo("DISABLE");
    }

    @Test @DisplayName("updateStatus_active_byUser_throwsUnauthorized")
    void updateStatus_active_byUser() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        UpdateBankAccountStatusRequest req = UpdateBankAccountStatusRequest.builder().status("ACTIVE").build();
        assertThatThrownBy(() -> service.updateStatus(10L, req, 1L, "USER"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("updateStatus_invalidStatus_throws")
    void updateStatus_invalid() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        UpdateBankAccountStatusRequest req = UpdateBankAccountStatusRequest.builder().status("UNKNOWN").build();
        assertThatThrownBy(() -> service.updateStatus(10L, req, 1L, "STAFF"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("getById_notFound_throws")
    void getById_notFound() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(10L, 1L, "USER")).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("getById_unauthorized_throws")
    void getById_unauth() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        assertThatThrownBy(() -> service.getById(10L, 999L, "USER")).isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("delete_owner_succeeds")
    void delete_ok() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        service.delete(10L, 1L, "USER");
        verify(bankAccountRepository).delete(account);
    }

    @Test @DisplayName("checkAccountExists_returnsBoolean")
    void checkAccount() {
        when(bankAccountRepository.existsByAccountNumberAndBankCodeAndUserIdNot("123", "VCB", 1L)).thenReturn(true);
        assertThat(service.checkAccountExists("123", "VCB", 1L)).isTrue();
    }

    @Test @DisplayName("update_byOwner_resetsVerification")
    void update_owner_reset() {
        when(bankAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(bankAccountRepository.existsByAccountNumberAndBankCodeAndUserIdNot(any(), any(), any())).thenReturn(false);
        when(bankAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateBankAccountRequest req = new UpdateBankAccountRequest();
        req.setBankCode("TCB"); req.setAccountNumber("999"); req.setAccountHolderName("X");
        BankAccountResponse r = service.update(10L, req, 1L, "USER");
        assertThat(r.getStatus()).isEqualTo("PENDING");
    }
}
