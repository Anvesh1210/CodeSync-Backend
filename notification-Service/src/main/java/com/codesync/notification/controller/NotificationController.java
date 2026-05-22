package com.codesync.notification.controller;

import com.codesync.notification.entity.Notification;
import com.codesync.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Resource", description = "Endpoints for managing system and user notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Get all notifications for a recipient")
    public ResponseEntity<List<Notification>> getByRecipient(@PathVariable String recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/recipient/{recipientId}/readAll")
    @Operation(summary = "Mark all notifications as read for a recipient")
    public ResponseEntity<Void> markAllRead(@PathVariable String recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/recipient/{recipientId}/read")
    @Operation(summary = "Delete all read notifications for a recipient")
    public ResponseEntity<Void> deleteRead(@PathVariable String recipientId) {
        notificationService.deleteRead(recipientId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recipient/{recipientId}/unreadCount")
    @Operation(summary = "Get count of unread notifications for a recipient")
    public ResponseEntity<Long> getUnreadCount(@PathVariable String recipientId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a specific notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable Integer notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @Operation(summary = "Send a single notification")
    public ResponseEntity<Void> send(@RequestBody Notification notification) {
        notificationService.send(notification);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send a broadcast notification to multiple recipients")
    public ResponseEntity<Void> sendBulk(@RequestBody List<String> recipientIds, @RequestParam String title, @RequestParam String message) {
        notificationService.sendBulk(recipientIds, title, message);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "Get all notifications platform-wide")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }
}
