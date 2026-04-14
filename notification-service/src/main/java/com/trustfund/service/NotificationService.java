package com.trustfund.service;

import com.trustfund.model.Notification;
import com.trustfund.exception.NotificationNotFoundException;
import com.trustfund.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustfund.model.request.NotificationRequest;
import com.trustfund.client.IdentityServiceClient;
import com.trustfund.service.EmailService;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final IdentityServiceClient identityServiceClient;
    private final EmailService emailService;

    public Notification createNotification(Notification notification) {
        if (notification.getIsRead() == null) {
            notification.setIsRead(false);
        }
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Notification markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + id));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public int getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public List<Notification> getLatestNotifications(Long userId) {
        return notificationRepository.findTop15ByUserIdOrderByCreatedAtDesc(userId);
    }

    public Notification createNotificationFromRequest(NotificationRequest request) {
        String jsonData = null;
        if (request.getData() != null) {
            try {
                jsonData = objectMapper.writeValueAsString(request.getData());
            } catch (Exception e) {
                log.error("Error serializing notification data", e);
            }
        }

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .title(request.getTitle())
                .content(request.getContent())
                .data(jsonData)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Logic gửi Email nếu là cảnh báo pháp lý
        if ("LEGAL_WARNING".equals(request.getType())) {
            try {
                com.trustfund.dto.UserInfoResponse user = identityServiceClient.getUserInfo(request.getUserId());
                if (user != null && user.getEmail() != null) {
                    emailService.sendLegalWarningEmail(
                            user.getEmail(),
                            user.getFullName(),
                            request.getTitle().replace("CẢNH BÁO PHÁP LÝ: CHIẾN DỊCH ", "").replace("\"", ""),
                            request.getContent()
                    );
                }
            } catch (Exception e) {
                log.error("Failed to send legal warning email: {}", e.getMessage());
            }
        }

        return saved;
    }
}
