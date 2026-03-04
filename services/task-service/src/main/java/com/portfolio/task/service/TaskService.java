package com.portfolio.task.service;

import com.portfolio.common.event.TaskEvent;
import com.portfolio.common.security.UserContext;
import com.portfolio.task.dto.TaskRequest;
import com.portfolio.task.dto.TaskResponse;
import com.portfolio.task.entity.Task;
import com.portfolio.task.kafka.TaskEventProducer;
import com.portfolio.task.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskEventProducer producer;

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksForUser(UserContext user, Pageable pageable) {
        return taskRepository
                .findByOwnerIdOrAssignedToId(user.userId(), user.userId(), pageable)
                .map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id, UserContext user) {
        Task task = findOwnedOrAssigned(id, user.userId());
        return TaskResponse.from(task);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @Transactional
    public TaskResponse createTask(TaskRequest req, UserContext user) {
        Task task = Task.builder()
                .title(req.title())
                .description(req.description())
                .status(req.status())
                .priority(req.priority())
                .dueDate(req.dueDate())
                .ownerId(user.userId())
                .ownerEmail(user.email())
                .ownerName(user.fullName())
                .assignedToId(req.assignedToId())
                .assignedToName(req.assignedToName())
                .build();

        task = taskRepository.save(task);

        producer.publish(TaskEvent.TOPIC_CREATED, buildEvent("CREATED", task, null));
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest req, UserContext user) {
        Task task = taskRepository.findByIdAndOwnerId(id, user.userId())
                .orElseThrow(() -> new EntityNotFoundException("Task not found or access denied"));

        String oldStatus = task.getStatus().name();

        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setStatus(req.status());
        task.setPriority(req.priority());
        task.setDueDate(req.dueDate());
        task.setAssignedToId(req.assignedToId());
        task.setAssignedToName(req.assignedToName());

        task = taskRepository.save(task);

        producer.publish(TaskEvent.TOPIC_UPDATED, buildEvent("UPDATED", task, oldStatus));
        return TaskResponse.from(task);
    }

    @Transactional
    public void deleteTask(Long id, UserContext user) {
        Task task = taskRepository.findByIdAndOwnerId(id, user.userId())
                .orElseThrow(() -> new EntityNotFoundException("Task not found or access denied"));

        TaskEvent event = buildEvent("DELETED", task, null);
        taskRepository.delete(task);
        producer.publish(TaskEvent.TOPIC_DELETED, event);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Task findOwnedOrAssigned(Long taskId, Long userId) {
        return taskRepository.findById(taskId)
                .filter(t -> userId.equals(t.getOwnerId()) || userId.equals(t.getAssignedToId()))
                .orElseThrow(() -> new EntityNotFoundException("Task not found or access denied"));
    }

    private TaskEvent buildEvent(String eventType, Task task, String oldStatus) {
        return new TaskEvent(
                eventType,
                task.getId(),
                task.getTitle(),
                oldStatus,
                task.getStatus().name(),
                task.getPriority().name(),
                task.getOwnerId(),
                task.getOwnerEmail(),
                task.getOwnerName(),
                LocalDateTime.now()
        );
    }
}
