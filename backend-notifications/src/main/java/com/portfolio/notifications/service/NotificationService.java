package com.portfolio.notifications.service;

import com.portfolio.notifications.dto.NotificationResponse;
import com.portfolio.notifications.entity.Notification;
import com.portfolio.notifications.entity.User;
import com.portfolio.notifications.kafka.TaskEvent;
import com.portfolio.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core business logic for the notification service.
 *
 * Translates incoming {@link TaskEvent}s into human-readable
 * {@link Notification} records and persists them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final int MAX_NOTIFICATIONS_PER_PAGE = 20;

    private final NotificationRepository notificationRepository;

    // ── Kafka event handlers ──────────────────────────────────────────

    @Transactional
    public void handleTaskCreated(TaskEvent event) {
        String message = String.format(
                "✅ Task '%s' was created with %s priority.",
                event.taskTitle(), formatPriority(event.priority()));

        persist(event, "TASK_CREATED", message);
        log.info("Notification persisted — TASK_CREATED taskId={}", event.taskId());
    }

    @Transactional
    public void handleTaskUpdated(TaskEvent event) {
        String message;

        boolean statusChanged = event.oldStatus() != null
                && event.newStatus() != null
                && !event.oldStatus().equals(event.newStatus());

        if (statusChanged) {
            message = String.format(
                    "🔄 Task '%s' moved from %s → %s.",
                    event.taskTitle(),
                    formatStatus(event.oldStatus()),
                    formatStatus(event.newStatus()));
        } else {
            message = String.format(
                    "✏️ Task '%s' was updated.",
                    event.taskTitle());
        }

        persist(event, "TASK_UPDATED", message);
        log.info("Notification persisted — TASK_UPDATED taskId={} statusChanged={}",
                event.taskId(), statusChanged);
    }

    @Transactional
    public void handleTaskDeleted(TaskEvent event) {
        String message = String.format(
                "🗑️ Task '%s' was deleted.",
                event.taskTitle());

        persist(event, "TASK_DELETED", message);
        log.info("Notification persisted — TASK_DELETED taskId={}", event.taskId());
    }

    // ── REST query methods ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(User user) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(),
                        PageRequest.of(0, MAX_NOTIFICATIONS_PER_PAGE))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForUser(User user) {
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public int markAllReadForUser(User user) {
        int updated = notificationRepository.markAllReadByUserId(user.getId());
        log.debug("Marked {} notifications as read for userId={}", updated, user.getId());
        return updated;
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private void persist(TaskEvent event, String eventType, String message) {
        Notification notification = Notification.builder()
                .eventType(eventType)
                .taskId(event.taskId())
                .taskTitle(event.taskTitle())
                .message(message)
                .userId(event.ownerId())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case "TODO"        -> "To Do";
            case "IN_PROGRESS" -> "In Progress";
            case "DONE"        -> "Done";
            default            -> status;
        };
    }

    private String formatPriority(String priority) {
        if (priority == null) return "medium";
        return priority.charAt(0) + priority.substring(1).toLowerCase();
    }
}
