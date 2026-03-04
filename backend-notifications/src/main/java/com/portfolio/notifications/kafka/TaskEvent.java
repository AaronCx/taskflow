package com.portfolio.notifications.kafka;

import java.time.LocalDateTime;

/**
 * Mirror of the task-manager's {@code TaskEvent} record.
 *
 * Must stay in sync with the producer — field names are used for JSON
 * deserialisation by Jackson / {@code JsonDeserializer}.
 */
public record TaskEvent(
        String        eventType,
        Long          taskId,
        String        taskTitle,
        String        oldStatus,
        String        newStatus,
        String        priority,
        Long          ownerId,
        String        ownerEmail,
        String        ownerName,
        LocalDateTime occurredAt
) {
    public static final String TOPIC_CREATED = "task.created";
    public static final String TOPIC_UPDATED = "task.updated";
    public static final String TOPIC_DELETED = "task.deleted";
}
