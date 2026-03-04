package com.portfolio.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user_id",    columnList = "user_id"),
    @Index(name = "idx_notifications_is_read",    columnList = "is_read"),
    @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String eventType;   // CREATED | UPDATED | DELETED

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private String taskTitle;

    /** Human-readable message, e.g. "Task 'Fix login' moved TODO → IN_PROGRESS." */
    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
