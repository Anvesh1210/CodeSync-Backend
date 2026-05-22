package com.codesync.notification.service;

import com.codesync.notification.entity.Notification;
import com.codesync.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification mockNotification;
    private String recipientId;

    @BeforeEach
    void setUp() {
        recipientId = "user-123";
        mockNotification = Notification.builder()
                .notificationId(1)
                .recipientId(recipientId)
                .title("Test Title")
                .message("Test Message")
                .isRead(false)
                .build();
    }

    @Test
    void send_ShouldSaveAndBroadcast() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        notificationService.send(mockNotification);

        verify(notificationRepository).save(mockNotification);
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + recipientId), eq(mockNotification));
    }

    @Test
    void sendBulk_ShouldSendToAll() {
        List<String> recipients = Arrays.asList("u1", "u2");
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.sendBulk(recipients, "Title", "Msg");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void markAsRead_ShouldUpdateStatus() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(mockNotification));

        notificationService.markAsRead(1);

        assertTrue(mockNotification.isRead());
        verify(notificationRepository).save(mockNotification);
    }

    @Test
    void markAllRead_ShouldUpdateAll() {
        when(notificationRepository.findByRecipientIdAndIsRead(recipientId, false))
                .thenReturn(Arrays.asList(mockNotification));

        notificationService.markAllRead(recipientId);

        assertTrue(mockNotification.isRead());
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void deleteRead_ShouldCallRepository() {
        notificationService.deleteRead(recipientId);
        verify(notificationRepository).deleteByRecipientIdAndIsRead(recipientId, true);
    }

    @Test
    void getByRecipient_ShouldReturnList() {
        when(notificationRepository.findByRecipientId(recipientId)).thenReturn(Arrays.asList(mockNotification));

        List<Notification> result = notificationService.getByRecipient(recipientId);

        assertEquals(1, result.size());
    }

    @Test
    void getUnreadCount_ShouldReturnCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(recipientId, false)).thenReturn(5L);

        long result = notificationService.getUnreadCount(recipientId);

        assertEquals(5L, result);
    }

    @Test
    void deleteNotification_ShouldCallRepository() {
        notificationService.deleteNotification(1);
        verify(notificationRepository).deleteByNotificationId(1);
    }

    @Test
    void sendEmail_ShouldCallSender() {
        notificationService.sendEmail("to@ex.com", "Sub", "Body");
        verify(emailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void getAll_ShouldReturnList() {
        when(notificationRepository.findAll()).thenReturn(Arrays.asList(mockNotification));

        List<Notification> result = notificationService.getAll();

        assertEquals(1, result.size());
    }
}
