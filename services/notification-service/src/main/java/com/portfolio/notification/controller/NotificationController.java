package com.portfolio.notification.controller;

import com.portfolio.notification.entity.Notification;
import com.portfolio.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Notification endpoints consumed by the frontend.
 * User identity comes from {@code X-User-Id} header injected by the gateway.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Per-user notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List the 20 most recent notifications for the current user")
    public Page<Notification> getNotifications(
            @RequestHeader("X-User-Id") Long userId) {
        return notificationService.getNotificationsForUser(userId);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Return the number of unread notifications")
    public Map<String, Long> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {
        return Map.of("count", notificationService.getUnreadCount(userId));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read for the current user")
    public Map<String, Integer> markAllRead(
            @RequestHeader("X-User-Id") Long userId) {
        return Map.of("marked", notificationService.markAllRead(userId));
    }
}
