package com.portfolio.taskmanager.kafka;

import java.time.LocalDateTime;

/**
 * Immutable event published to Kafka whenever a task changes.
 *
 * Serialised as JSON by {@link org.springframework.kafka.support.serializer.JsonSerializer}.
 * The notification-service deserialises this same class via {@code JsonDeserializer}.
 *
 * @param eventType   One of: TASK_CREATED | TASK_UPDATED | TASK_DELETED
 * @param taskId      Database ID of the affected task
 * @param taskTitle   Human-readable title (snapshot at event time)
 * @param oldStatus   Previous status — null for TASK_CREATED events
 * @param newStatus   Current status  — null for TASK_DELETED events
 * @param priority    Task priority at event time
 * @param ownerId     User ID of the task owner (recipient of the notification)
 * @param ownerEmail  Used by the notification service to look up the user
 * @param ownerName   Full name for display in notification messages
 * @param occurredAt  Wall-clock timestamp when the event was generated
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
    // Topic name constants — single source of truth shared via the event class
    public static final String TOPIC_CREATED = "task.created";
    public static final String TOPIC_UPDATED = "task.updated";
    public static final String TOPIC_DELETED = "task.deleted";
}
