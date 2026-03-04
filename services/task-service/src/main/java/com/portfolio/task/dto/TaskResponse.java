package com.portfolio.task.dto;

import com.portfolio.task.entity.Priority;
import com.portfolio.task.entity.Task;
import com.portfolio.task.entity.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDateTime dueDate,
        Long ownerId,
        String ownerEmail,
        String ownerName,
        Long assignedToId,
        String assignedToName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getPriority(), task.getDueDate(),
                task.getOwnerId(), task.getOwnerEmail(), task.getOwnerName(),
                task.getAssignedToId(), task.getAssignedToName(),
                task.getCreatedAt(), task.getUpdatedAt()
        );
    }
}
