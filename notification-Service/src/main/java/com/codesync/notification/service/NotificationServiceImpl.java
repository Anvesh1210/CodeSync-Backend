package com.codesync.notification.service;

import com.codesync.notification.entity.Notification;
import com.codesync.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender emailSender;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void send(Notification notification) {
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications/" + saved.getRecipientId(), saved);
    }

    @Override
    public void sendBulk(List<String> recipientIds, String title, String message) {
        for (String recipientId : recipientIds) {
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .title(title)
                    .message(message)
                    .type("BROADCAST")
                    .isRead(false)
                    .build();
            this.send(notification);
        }
    }

    @Override
    public void markAsRead(Integer notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Override
    public void markAllRead(String recipientId) {
        List<Notification> unreadNotifications = notificationRepository.findByRecipientIdAndIsRead(recipientId, false);
        unreadNotifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    public void deleteRead(String recipientId) {
        notificationRepository.deleteByRecipientIdAndIsRead(recipientId, true);
    }

    @Override
    public List<Notification> getByRecipient(String recipientId) {
        return notificationRepository.findByRecipientId(recipientId);
    }

    @Override
    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public void deleteNotification(Integer notificationId) {
        notificationRepository.deleteByNotificationId(notificationId);
    }

    @Override
    public void sendEmail(String toAddress, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toAddress);
        message.setSubject(subject);
        message.setText(body);
        emailSender.send(message);
    }

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }
}
