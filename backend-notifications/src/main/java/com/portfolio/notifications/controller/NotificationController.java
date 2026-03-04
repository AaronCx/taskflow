package com.portfolio.notifications.controller;

import com.portfolio.notifications.dto.NotificationResponse;
import com.portfolio.notifications.entity.User;
import com.portfolio.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the notification feed.
 *
 * All endpoints require a valid JWT (same secret as the task-manager service).
 *
 * GET  /api/notifications              → recent notifications for current user
 * GET  /api/notifications/unread-count → badge count
 * PUT  /api/notifications/read-all     → mark all as read
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Task event notification feed")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get recent notifications (up to 20) for the authenticated user")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                notificationService.getNotificationsForUser(currentUser));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get the count of unread notifications (for the bell badge)")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {

        long count = notificationService.getUnreadCountForUser(currentUser);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllRead(
            @AuthenticationPrincipal User currentUser) {

        int updated = notificationService.markAllReadForUser(currentUser);
        return ResponseEntity.ok(Map.of("marked", updated));
    }
}
