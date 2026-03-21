package com.trustfund.repository;

import com.trustfund.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    int countByUserIdAndIsReadFalse(Long userId);

    List<Notification> findTop15ByUserIdOrderByCreatedAtDesc(Long userId);
}
