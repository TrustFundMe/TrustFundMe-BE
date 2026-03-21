package com.trustfund.controller;

import com.trustfund.model.Notification;
import com.trustfund.service.NotificationService;
import com.trustfund.model.request.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody Notification notification) {
        return ResponseEntity.ok(notificationService.createNotification(notification));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<List<Notification>> getLatestNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getLatestNotifications(userId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PostMapping("/event")
    public ResponseEntity<Notification> createEventNotification(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.createNotificationFromRequest(request));
    }
}
