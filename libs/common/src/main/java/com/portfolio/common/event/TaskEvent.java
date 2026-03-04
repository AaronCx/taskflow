package com.portfolio.common.event;

import java.time.LocalDateTime;

/**
 * Kafka event payload shared between task-service (producer)
 * and notification-service (consumer).
 * <p>
 * Must be JSON-serializable — all fields use basic Java types.
 */
public record TaskEvent(
        String eventType,
        Long taskId,
        String taskTitle,
        String oldStatus,    // null for CREATED events
        String newStatus,
        String priority,
        Long ownerId,
        String ownerEmail,
        String ownerName,
        LocalDateTime occurredAt
) {
    /** Kafka topic names — use these constants everywhere instead of raw strings. */
    public static final String TOPIC_CREATED = "task.created";
    public static final String TOPIC_UPDATED = "task.updated";
    public static final String TOPIC_DELETED = "task.deleted";
}
