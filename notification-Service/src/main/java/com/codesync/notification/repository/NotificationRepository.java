package com.codesync.notification.repository;

import com.codesync.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByRecipientId(String recipientId);
    List<Notification> findByRecipientIdAndIsRead(String recipientId, boolean isRead);
    long countByRecipientIdAndIsRead(String recipientId, boolean isRead);
    List<Notification> findByType(String type);
    List<Notification> findByRelatedId(String relatedId);
    
    @Transactional
    void deleteByNotificationId(Integer notificationId);
    
    @Transactional
    void deleteByRecipientIdAndIsRead(String recipientId, boolean isRead);
}
