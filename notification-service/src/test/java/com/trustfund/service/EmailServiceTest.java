package com.trustfund.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@trustfund.me");
    }

    @Nested
    @DisplayName("sendEmail")
    class SendEmail {

        @Test
        @DisplayName("creates MimeMessage and sends when fromEmail is configured")
        void createsAndSendsMessage_whenConfigured() {
            MimeMessage mockMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mockMessage);

            emailService.sendEmail("user@example.com", "Test Subject", "<html>content</html>");

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mockMessage);
        }

        @Test
        @DisplayName("skips send when fromEmail is blank")
        void skipsSend_whenFromEmailIsBlank() {
            ReflectionTestUtils.setField(emailService, "fromEmail", "   ");

            emailService.sendEmail("user@example.com", "Subject", "Content");

            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("skips send when fromEmail is null")
        void skipsSend_whenFromEmailIsNull() {
            ReflectionTestUtils.setField(emailService, "fromEmail", null);

            emailService.sendEmail("user@example.com", "Subject", "Content");

            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendLegalWarningEmail")
    class SendLegalWarningEmail {

        @Test
        @DisplayName("creates and sends legal warning email")
        void createsAndSendsLegalWarningEmail() {
            MimeMessage mockMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mockMessage);

            emailService.sendLegalWarningEmail("violator@example.com", "Nguyen Van A",
                    "Chiến dịch Từ Thiện", "Minh chứng không hợp lệ.");

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mockMessage);
        }

        @Test
        @DisplayName("handles null userName without throwing")
        void handlesNullUserName_withoutThrowing() {
            MimeMessage mockMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mockMessage);

            assertThatCode(() ->
                    emailService.sendLegalWarningEmail("to@example.com", null, "Campaign", "Content"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("handles blank userName without throwing")
        void handlesBlankUserName_withoutThrowing() {
            MimeMessage mockMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mockMessage);

            assertThatCode(() ->
                    emailService.sendLegalWarningEmail("to@example.com", "  ", "Campaign", "Content"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("sendCommitmentRequestEmail")
    class SendCommitmentRequestEmail {

        @Test
        @DisplayName("creates and sends commitment request email")
        void createsAndSendsCommitmentRequestEmail() {
            MimeMessage mockMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mockMessage);

            emailService.sendCommitmentRequestEmail("fundowner@example.com", "Tran Thi B",
                    "Chiến dịch Học Bổng 2026", 42L);

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mockMessage);
        }

        @Test
        @DisplayName("handles null userName without throwing")
        void handlesNullUserName_withoutThrowing() {
            MimeMessage mockMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mockMessage);

            assertThatCode(() ->
                    emailService.sendCommitmentRequestEmail("to@example.com", null, "Campaign", 1L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("sendCommitmentSuccessEmail")
    class SendCommitmentSuccessEmail {

        @Test
        @DisplayName("creates and sends commitment success email")
        void createsAndSendsCommitmentSuccessEmail() {
            MimeMessage mockMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mockMessage);

            emailService.sendCommitmentSuccessEmail("fundowner@example.com", "Le Van C",
                    "Chiến dịch Nhà An Cư");

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mockMessage);
        }

        @Test
        @DisplayName("handles null userName without throwing")
        void handlesNullUserName_withoutThrowing() {
            MimeMessage mockMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mockMessage);

            assertThatCode(() ->
                    emailService.sendCommitmentSuccessEmail("to@example.com", null, "Campaign"))
                    .doesNotThrowAnyException();
        }
    }
}
