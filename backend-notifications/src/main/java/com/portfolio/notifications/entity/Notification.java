package com.portfolio.notifications.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persisted notification record.
 *
 * One notification is created per Kafka event received.
 * The {@code userId} field scopes it to the task owner.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_id",  columnList = "user_id"),
        @Index(name = "idx_notifications_is_read",  columnList = "is_read"),
        @Index(name = "idx_notifications_created",  columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** TASK_CREATED | TASK_UPDATED | TASK_DELETED */
    @Column(nullable = false, length = 30)
    private String eventType;

    /** ID of the task that triggered this notification. */
    private Long taskId;

    /** Human-readable title of the task at event time. */
    @Column(length = 255)
    private String taskTitle;

    /** Display message shown in the notification feed. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** User this notification belongs to (task owner). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** False until the user opens the notification feed. */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
