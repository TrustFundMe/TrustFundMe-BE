package com.trustfund.service;

import com.trustfund.service.implementServices.EmailServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Session;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks private EmailServiceImpl service;

    private MimeMessage fakeMessage;

    @BeforeEach
    void setUp() {
        fakeMessage = new MimeMessage(Session.getInstance(new Properties()));
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@trustfundme.com");
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");
    }

    @Test @DisplayName("sendOtpEmail_sendsMime")
    void sendOtp_ok() {
        when(mailSender.createMimeMessage()).thenReturn(fakeMessage);
        service.sendOtpEmail("u@e.com", "123456", "User", "verify_email");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test @DisplayName("sendOtpEmail_missingFrom_throws")
    void sendOtp_missing() {
        ReflectionTestUtils.setField(service, "fromEmail", "");
        assertThatThrownBy(() -> service.sendOtpEmail("u@e.com", "1", "X", "verify_email"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test @DisplayName("sendOtpEmail_resetPassword_purpose")
    void sendOtp_reset() {
        when(mailSender.createMimeMessage()).thenReturn(fakeMessage);
        service.sendOtpEmail("u@e.com", "111", "Name", "reset_password");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test @DisplayName("sendCommitmentRequestEmail_sendsMail")
    void sendCommitment_ok() {
        when(mailSender.createMimeMessage()).thenReturn(fakeMessage);
        service.sendCommitmentRequestEmail("u@e.com", "owner", "camp", 10L,
                "A", "HN", "wp", "tax", "id", "1/1/2020", "HCM", "09", "http://fe");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test @DisplayName("sendCommitmentRequestEmail_fallbackBaseUrl")
    void sendCommitment_nullBaseUrl() {
        when(mailSender.createMimeMessage()).thenReturn(fakeMessage);
        service.sendCommitmentRequestEmail("u@e.com", "o", "c", 1L,
                null, null, null, null, null, null, null, null, null);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test @DisplayName("sendCommitmentRequestEmail_missingConfig_throws")
    void sendCommitment_missingCfg() {
        ReflectionTestUtils.setField(service, "fromEmail", "");
        assertThatThrownBy(() -> service.sendCommitmentRequestEmail("u@e.com","o","c",1L,
                null,null,null,null,null,null,null,null,null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test @DisplayName("sendEvidenceReminder_sendsMail")
    void sendEvidence_ok() {
        when(mailSender.createMimeMessage()).thenReturn(fakeMessage);
        service.sendEvidenceReminder("u@e.com", "owner", "camp", "plan", BigDecimal.valueOf(100), LocalDateTime.now());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test @DisplayName("sendEvidenceReminder_nullFields_ok")
    void sendEvidence_nullFields() {
        when(mailSender.createMimeMessage()).thenReturn(fakeMessage);
        service.sendEvidenceReminder("u@e.com", null, "camp", "plan", null, null);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test @DisplayName("sendFraudReport_sendsMail")
    void sendFraud_ok() {
        when(mailSender.createMimeMessage()).thenReturn(fakeMessage);
        service.sendFraudReport("u@e.com", "owner", "camp", "reason", "evidence");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test @DisplayName("sendCommitmentSuccessEmail_sendsMail")
    void sendSuccess_ok() {
        when(mailSender.createMimeMessage()).thenReturn(fakeMessage);
        service.sendCommitmentSuccessEmail("u@e.com", "owner", "camp");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test @DisplayName("sendCommitmentSuccessEmail_missingConfig_skipsSilently")
    void sendSuccess_missingCfg() {
        ReflectionTestUtils.setField(service, "fromEmail", "");
        service.sendCommitmentSuccessEmail("u@e.com", "o", "c");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
