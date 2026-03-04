package com.portfolio.task.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Task record stored in {@code task_db}.
 * <p>
 * User identity is denormalized (owner_id, owner_name) rather than using a
 * foreign key across service boundaries — this is standard database-per-service
 * practice.
 */
@Entity
@Table(name = "tasks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    private LocalDateTime dueDate;

    // ── Owner identity (from gateway X-User-* headers) ────────────────────────
    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private String ownerName;

    // ── Optional assignee (denormalized) ──────────────────────────────────────
    private Long assignedToId;
    private String assignedToName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
