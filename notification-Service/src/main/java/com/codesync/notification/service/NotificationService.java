package com.codesync.notification.service;

import com.codesync.notification.entity.Notification;

import java.util.List;

public interface NotificationService {
    void send(Notification notification);
    void sendBulk(List<String> recipientIds, String title, String message);
    void markAsRead(Integer notificationId);
    void markAllRead(String recipientId);
    void deleteRead(String recipientId);
    List<Notification> getByRecipient(String recipientId);
    long getUnreadCount(String recipientId);
    void deleteNotification(Integer notificationId);
    void sendEmail(String toAddress, String subject, String body);
    List<Notification> getAll();
}
