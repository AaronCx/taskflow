package com.portfolio.taskmanager.service;

import com.portfolio.taskmanager.dto.request.TaskRequest;
import com.portfolio.taskmanager.dto.response.TaskResponse;
import com.portfolio.taskmanager.entity.Task;
import com.portfolio.taskmanager.entity.User;
import com.portfolio.taskmanager.enums.TaskPriority;
import com.portfolio.taskmanager.enums.TaskStatus;
import com.portfolio.taskmanager.exception.ResourceNotFoundException;
import com.portfolio.taskmanager.kafka.TaskEvent;
import com.portfolio.taskmanager.kafka.TaskEventProducer;
import com.portfolio.taskmanager.repository.TaskRepository;
import com.portfolio.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for task CRUD operations.
 *
 * All mutations are scoped to the authenticated user — a user can only
 * modify/delete tasks they own, enforced via {@code findByIdAndOwner}.
 *
 * After each successful mutation a Kafka event is published (fire-and-forget).
 * If Kafka is unavailable the task operation still completes — the event is
 * simply dropped with a WARN log.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository    taskRepository;
    private final UserRepository    userRepository;
    private final TaskEventProducer eventProducer;

    // ── Read ──────────────────────────────────────────────────────────

    /** Return all tasks owned by the caller, newest first. */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForUser(User currentUser) {
        return taskRepository.findByOwnerOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    /** Return all tasks owned by the caller filtered by status. */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByStatus(User currentUser, TaskStatus status) {
        return taskRepository.findByOwnerAndStatusOrderByCreatedAtDesc(currentUser, status)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    /** Return a single task, validating ownership. */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, User currentUser) {
        Task task = findOwnedTask(id, currentUser);
        return TaskResponse.from(task);
    }

    // ── Write ─────────────────────────────────────────────────────────

    @Transactional
    public TaskResponse createTask(TaskRequest request, User currentUser) {
        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .dueDate(request.dueDate())
                .owner(currentUser)
                .assignedTo(resolveAssignee(request.assignedToId()))
                .build();

        Task saved = taskRepository.save(task);

        // Publish event after the DB transaction commits
        eventProducer.publish(TaskEvent.TOPIC_CREATED, buildEvent(
                "TASK_CREATED", saved, null, saved.getStatus().name(), currentUser));

        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request, User currentUser) {
        Task task = findOwnedTask(id, currentUser);

        // Capture old status before mutation
        String oldStatus = task.getStatus().name();

        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.status()   != null) task.setStatus(request.status());
        if (request.priority() != null) task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setAssignedTo(resolveAssignee(request.assignedToId()));

        Task saved = taskRepository.save(task);
        String newStatus = saved.getStatus().name();

        // Publish on every update — includes old/new status so the consumer
        // can decide whether the change is worth notifying the user about
        eventProducer.publish(TaskEvent.TOPIC_UPDATED, buildEvent(
                "TASK_UPDATED", saved, oldStatus, newStatus, currentUser));

        return TaskResponse.from(saved);
    }

    @Transactional
    public void deleteTask(Long id, User currentUser) {
        Task task = findOwnedTask(id, currentUser);

        // Capture data before deletion
        TaskEvent event = buildEvent(
                "TASK_DELETED", task, task.getStatus().name(), null, currentUser);

        taskRepository.delete(task);

        // Publish after successful delete
        eventProducer.publish(TaskEvent.TOPIC_DELETED, event);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Fetches a task and asserts that the given user owns it. */
    private Task findOwnedTask(Long id, User owner) {
        return taskRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    /** Optionally resolve an assignee ID to a User entity. */
    private User resolveAssignee(Long assignedToId) {
        if (assignedToId == null) return null;
        return userRepository.findById(assignedToId)
                .orElseThrow(() -> new ResourceNotFoundException("User", assignedToId));
    }

    /** Construct a {@link TaskEvent} from the current state of the task. */
    private TaskEvent buildEvent(String type, Task task, String oldStatus,
                                 String newStatus, User owner) {
        return new TaskEvent(
                type,
                task.getId(),
                task.getTitle(),
                oldStatus,
                newStatus,
                task.getPriority().name(),
                owner.getId(),
                owner.getEmail(),
                owner.getFirstName() + " " + owner.getLastName(),
                LocalDateTime.now()
        );
    }
}
