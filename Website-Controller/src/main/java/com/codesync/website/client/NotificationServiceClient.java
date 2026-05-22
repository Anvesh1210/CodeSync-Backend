package com.codesync.website.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @GetMapping("/notifications/recipient/{recipientId}")
    Object getUserNotifications(@PathVariable("recipientId") String recipientId);

    @PostMapping("/notifications")
    Object sendPlatformNotification(@RequestBody Object notification);

    @PostMapping("/notifications/bulk")
    void sendBulk(@RequestBody java.util.List<String> recipientIds, @RequestParam("title") String title, @RequestParam("message") String message);

    @PutMapping("/notifications/{notificationId}/read")
    void markAsRead(@PathVariable("notificationId") Integer notificationId);

    @PutMapping("/notifications/recipient/{recipientId}/readAll")
    void markAllRead(@PathVariable("recipientId") String recipientId);
}
