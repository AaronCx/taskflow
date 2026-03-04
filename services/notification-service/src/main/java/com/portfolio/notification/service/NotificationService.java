package com.portfolio.notification.service;

import com.portfolio.common.event.TaskEvent;
import com.portfolio.notification.entity.Notification;
import com.portfolio.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    // ── Kafka event handlers ──────────────────────────────────────────────────

    @Transactional
    public void handleTaskCreated(TaskEvent event) {
        save(event, "CREATED",
             String.format("Task '%s' was created with %s priority.",
                           event.taskTitle(), event.priority()));
    }

    @Transactional
    public void handleTaskUpdated(TaskEvent event) {
        String message = (event.oldStatus() != null && !event.oldStatus().equals(event.newStatus()))
                ? String.format("Task '%s' moved from %s to %s.",
                                event.taskTitle(), event.oldStatus(), event.newStatus())
                : String.format("Task '%s' was updated.",
                                event.taskTitle());
        save(event, "UPDATED", message);
    }

    @Transactional
    public void handleTaskDeleted(TaskEvent event) {
        save(event, "DELETED",
             String.format("Task '%s' was deleted.", event.taskTitle()));
    }

    // ── REST handlers ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsForUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllReadByUserId(userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void save(TaskEvent event, String eventType, String message) {
        Notification n = Notification.builder()
                .userId(event.ownerId())
                .eventType(eventType)
                .taskId(event.taskId())
                .taskTitle(event.taskTitle())
                .message(message)
                .build();
        repository.save(n);
        log.debug("Saved notification for user {}: {}", event.ownerId(), message);
    }
}
