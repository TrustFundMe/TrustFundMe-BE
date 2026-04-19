package com.trustfund.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustfund.client.IdentityServiceClient;
import com.trustfund.dto.UserInfoResponse;
import com.trustfund.exception.NotificationNotFoundException;
import com.trustfund.model.Notification;
import com.trustfund.model.request.NotificationRequest;
import com.trustfund.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @Mock
    private EmailService emailService;

    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, objectMapper, identityServiceClient, emailService);
    }

    @Nested
    @DisplayName("createNotification")
    class CreateNotification {

        @Test
        @DisplayName("saves notification with isRead=false when isRead is null")
        void savesNotificationWithIsReadFalse_whenIsReadIsNull() {
            Notification input = Notification.builder()
                    .userId(1L).title("Test Title").content("Test Content").type("GENERAL").build();

            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(10L);
                return n;
            });

            Notification result = notificationService.createNotification(input);

            verify(notificationRepository).save(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().getIsRead()).isFalse();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getIsRead()).isFalse();
        }

        @Test
        @DisplayName("preserves isRead when already set")
        void preservesIsRead_whenAlreadySet() {
            Notification input = Notification.builder()
                    .userId(1L).title("Test").isRead(true).build();

            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

            notificationService.createNotification(input);

            verify(notificationRepository).save(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().getIsRead()).isTrue();
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("sets readAt timestamp and returns notification when found")
        void setsReadAtAndReturnsNotification_whenFound() {
            Notification existing = Notification.builder()
                    .id(5L).userId(1L).title("Test").isRead(false).build();

            when(notificationRepository.findById(5L)).thenReturn(java.util.Optional.of(existing));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

            Notification result = notificationService.markAsRead(5L);

            assertThat(result.getIsRead()).isTrue();
            assertThat(result.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("throws NotificationNotFoundException when not found")
        void throwsNotificationNotFoundException_whenNotFound() {
            when(notificationRepository.findById(999L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(999L))
                    .isInstanceOf(NotificationNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("returns count of unread notifications for user")
        void returnsCountOfUnread() {
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(7);

            int count = notificationService.getUnreadCount(1L);

            assertThat(count).isEqualTo(7);
        }

        @Test
        @DisplayName("returns zero when no unread notifications")
        void returnsZero_whenNoUnread() {
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(0);

            assertThat(notificationService.getUnreadCount(1L)).isZero();
        }
    }

    @Nested
    @DisplayName("getLatestNotifications")
    class GetLatestNotifications {

        @Test
        @DisplayName("returns top 15 notifications for user")
        void returnsTop15Notifications() {
            List<Notification> notifications = List.of(
                    Notification.builder().id(1L).userId(1L).title("Notif 1").isRead(false).build(),
                    Notification.builder().id(2L).userId(1L).title("Notif 2").isRead(true).build()
            );
            when(notificationRepository.findTop15ByUserIdOrderByCreatedAtDesc(1L)).thenReturn(notifications);

            List<Notification> result = notificationService.getLatestNotifications(1L);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no notifications")
        void returnsEmptyList_whenNoNotifications() {
            when(notificationRepository.findTop15ByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Collections.emptyList());

            assertThat(notificationService.getLatestNotifications(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("createNotificationFromRequest")
    class CreateNotificationFromRequest {

        @Test
        @DisplayName("non-LEGAL_WARNING type saves notification without calling email service")
        void savesNotificationWithoutEmail_whenNotLegalWarning() throws Exception {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(1L).type("CAMPAIGN_UPDATE").targetId(100L).targetType("CAMPAIGN")
                    .title("Campaign Updated").content("Updated.")
                    .data(Map.of("campaignId", 100))
                    .build();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(1L);
                return n;
            });

            Notification result = notificationService.createNotificationFromRequest(request);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getType()).isEqualTo("CAMPAIGN_UPDATE");
            verify(identityServiceClient, never()).getUserInfo(any());
            verify(emailService, never()).sendLegalWarningEmail(any(), any(), any(), any());
        }

        @Test
        @DisplayName("LEGAL_WARNING type calls identityServiceClient and emailService")
        void sendsLegalWarningEmail_whenLegalWarningType() throws Exception {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(42L).type("LEGAL_WARNING")
                    .title("CẢNH BÁO PHÁP LÝ: CHIẾN DỊCH \"Test Campaign\"")
                    .content("You have violated terms.")
                    .build();

            UserInfoResponse userInfo = UserInfoResponse.builder()
                    .id(42L).fullName("John Doe").email("john@example.com").build();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(99L);
                return n;
            });
            when(identityServiceClient.getUserInfo(42L)).thenReturn(userInfo);

            Notification result = notificationService.createNotificationFromRequest(request);

            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getType()).isEqualTo("LEGAL_WARNING");
            verify(identityServiceClient).getUserInfo(42L);
            verify(emailService).sendLegalWarningEmail(
                    eq("john@example.com"),
                    eq("John Doe"),
                    eq("Test Campaign"),
                    eq("You have violated terms.")
            );
        }

        @Test
        @DisplayName("LEGAL_WARNING with email failure still saves notification (non-fatal)")
        void savesNotification_whenEmailFails() throws Exception {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(42L).type("LEGAL_WARNING")
                    .title("CẢNH BÁO PHÁP LÝ: CHIẾN DỊCH \"Test Campaign\"")
                    .content("Violation content.")
                    .build();

            UserInfoResponse userInfo = UserInfoResponse.builder()
                    .id(42L).fullName("Jane Doe").email("jane@example.com").build();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(55L);
                return n;
            });
            when(identityServiceClient.getUserInfo(42L)).thenReturn(userInfo);
            doThrow(new RuntimeException("SMTP error"))
                    .when(emailService).sendLegalWarningEmail(any(), any(), any(), any());

            Notification result = notificationService.createNotificationFromRequest(request);

            assertThat(result.getId()).isEqualTo(55L);
            assertThat(result.getType()).isEqualTo("LEGAL_WARNING");
        }

        @Test
        @DisplayName("LEGAL_WARNING when userInfo has null email does not send email")
        void doesNotSendEmail_whenUserInfoHasNullEmail() throws Exception {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(42L).type("LEGAL_WARNING")
                    .title("CẢNH BÁO PHÁP LÝ: CHIẾN DỊCH \"Test\"")
                    .content("Content")
                    .build();

            UserInfoResponse userInfo = UserInfoResponse.builder()
                    .id(42L).fullName("No Email User").email(null).build();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(1L);
                return n;
            });
            when(identityServiceClient.getUserInfo(42L)).thenReturn(userInfo);

            Notification result = notificationService.createNotificationFromRequest(request);

            assertThat(result.getId()).isEqualTo(1L);
            verify(emailService, never()).sendLegalWarningEmail(any(), any(), any(), any());
        }

        @Test
        @DisplayName("LEGAL_WARNING when userInfo returns null does not send email")
        void doesNotSendEmail_whenUserInfoReturnsNull() throws Exception {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(99L).type("LEGAL_WARNING")
                    .title("CẢNH BÁO PHÁP LÝ: CHIẾN DỊCH \"X\"")
                    .content("Content")
                    .build();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(2L);
                return n;
            });
            when(identityServiceClient.getUserInfo(99L)).thenReturn(null);

            Notification result = notificationService.createNotificationFromRequest(request);

            assertThat(result.getId()).isEqualTo(2L);
            verify(emailService, never()).sendLegalWarningEmail(any(), any(), any(), any());
        }
    }
}