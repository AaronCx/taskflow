package com.portfolio.notifications.dto;

import com.portfolio.notifications.entity.Notification;

import java.time.LocalDateTime;

/** Read-only DTO returned by the REST API. */
public record NotificationResponse(
        Long          id,
        String        eventType,
        Long          taskId,
        String        taskTitle,
        String        message,
        boolean       read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getEventType(),
                n.getTaskId(),
                n.getTaskTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
