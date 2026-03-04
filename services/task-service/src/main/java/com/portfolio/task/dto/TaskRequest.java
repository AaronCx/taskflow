package com.portfolio.task.dto;

import com.portfolio.task.entity.Priority;
import com.portfolio.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskRequest(
        @NotBlank String title,
        String description,
        @NotNull TaskStatus status,
        @NotNull Priority priority,
        LocalDateTime dueDate,
        Long assignedToId,
        String assignedToName
) {}
