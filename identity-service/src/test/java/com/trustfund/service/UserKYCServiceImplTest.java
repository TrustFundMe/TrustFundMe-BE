package com.trustfund.service;

import com.trustfund.client.NotificationServiceClient;
import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.User;
import com.trustfund.model.UserKYC;
import com.trustfund.model.enums.KYCStatus;
import com.trustfund.model.request.SubmitKYCRequest;
import com.trustfund.model.response.KYCResponse;
import com.trustfund.repository.BankAccountRepository;
import com.trustfund.repository.UserKYCRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.implementServices.UserKYCServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserKYCServiceImplTest {

    @Mock private UserKYCRepository userKYCRepository;
    @Mock private UserRepository userRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private NotificationServiceClient notificationServiceClient;

    @InjectMocks private UserKYCServiceImpl service;

    private User user;
    private UserKYC kyc;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("u@e.com").role(User.Role.USER).build();
        kyc = UserKYC.builder().id(5L).user(user).idNumber("123").status(KYCStatus.PENDING).build();
    }

    private SubmitKYCRequest req() {
        return SubmitKYCRequest.builder()
                .fullName("A").address("HN").idType("CCCD").idNumber("123")
                .issueDate(LocalDate.now().minusYears(1)).expiryDate(LocalDate.now().plusYears(5))
                .issuePlace("CA HN").idImageFront("f").idImageBack("b").selfieImage("s").build();
    }

    @Test @DisplayName("submitKYC_userNotFound_throws")
    void submit_noUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submitKYC(1L, req())).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("submitKYC_alreadySubmitted_throws")
    void submit_existing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userKYCRepository.existsByUserId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.submitKYC(1L, req())).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("submitKYC_duplicateIdNumberByOther_throws")
    void submit_dupId() {
        User other = User.builder().id(2L).build();
        UserKYC existing = UserKYC.builder().user(other).idNumber("123").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userKYCRepository.existsByUserId(1L)).thenReturn(false);
        when(userKYCRepository.findFirstByIdNumber("123")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.submitKYC(1L, req())).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("submitKYC_success_returnsPending")
    void submit_ok() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userKYCRepository.existsByUserId(1L)).thenReturn(false);
        when(userKYCRepository.findFirstByIdNumber(any())).thenReturn(Optional.empty());
        when(userKYCRepository.save(any())).thenAnswer(i -> { UserKYC k = i.getArgument(0); k.setId(5L); return k; });
        KYCResponse r = service.submitKYC(1L, req());
        assertThat(r.getStatus()).isEqualTo(KYCStatus.PENDING);
    }

    @Test @DisplayName("getMyKYC_notFound_throws")
    void getMy_notFound() {
        when(userKYCRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMyKYC(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("getMyKYC_ok")
    void getMy_ok() {
        when(userKYCRepository.findByUserId(1L)).thenReturn(Optional.of(kyc));
        assertThat(service.getMyKYC(1L).getId()).isEqualTo(5L);
    }

    @Test @DisplayName("resubmitKYC_approvedCannotResubmit")
    void resubmit_approved() {
        kyc.setStatus(KYCStatus.APPROVED);
        when(userKYCRepository.findByUserId(1L)).thenReturn(Optional.of(kyc));
        assertThatThrownBy(() -> service.resubmitKYC(1L, req())).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("resubmitKYC_success")
    void resubmit_ok() {
        when(userKYCRepository.findByUserId(1L)).thenReturn(Optional.of(kyc));
        when(userKYCRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        KYCResponse r = service.resubmitKYC(1L, req());
        assertThat(r.getStatus()).isEqualTo(KYCStatus.PENDING);
    }

    @Test @DisplayName("updateKYCStatus_notFound_throws")
    void update_notFound() {
        when(userKYCRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateKYCStatus(5L, KYCStatus.APPROVED, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("updateKYCStatus_rejectedWithoutReason_throws")
    void update_reject_noReason() {
        when(userKYCRepository.findById(5L)).thenReturn(Optional.of(kyc));
        assertThatThrownBy(() -> service.updateKYCStatus(5L, KYCStatus.REJECTED, ""))
                .isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("updateKYCStatus_approve_promotesUser")
    void update_approve_promote() {
        when(userKYCRepository.findById(5L)).thenReturn(Optional.of(kyc));
        when(userKYCRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        service.updateKYCStatus(5L, KYCStatus.APPROVED, null);
        assertThat(user.getRole()).isEqualTo(User.Role.FUND_OWNER);
        assertThat(user.getKycVerified()).isTrue();
    }

    @Test @DisplayName("updateKYCStatus_rejectedWithReason_ok")
    void update_reject_ok() {
        when(userKYCRepository.findById(5L)).thenReturn(Optional.of(kyc));
        when(userKYCRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        KYCResponse r = service.updateKYCStatus(5L, KYCStatus.REJECTED, "Bad image");
        assertThat(r.getStatus()).isEqualTo(KYCStatus.REJECTED);
        assertThat(r.getRejectionReason()).isEqualTo("Bad image");
    }

    @Test @DisplayName("getKYCStats_returnsCounts")
    void stats() {
        when(userRepository.count()).thenReturn(10L);
        when(userKYCRepository.countByStatus(KYCStatus.APPROVED)).thenReturn(3L);
        when(userKYCRepository.countByStatus(KYCStatus.PENDING)).thenReturn(2L);
        when(userKYCRepository.countByStatus(KYCStatus.REJECTED)).thenReturn(1L);
        assertThat(service.getKYCStats()).containsEntry("total", 10L).containsEntry("approved", 3L);
    }
}
