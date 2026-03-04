package com.portfolio.task.controller;

import com.portfolio.common.security.UserContext;
import com.portfolio.task.dto.TaskRequest;
import com.portfolio.task.dto.TaskResponse;
import com.portfolio.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Task CRUD controller.
 * <p>
 * User identity is read from headers injected by the API Gateway's
 * {@code JwtAuthenticationFilter}. This service does NOT validate JWTs.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "List tasks for the current user (paginated)")
    public Page<TaskResponse> listTasks(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Name") String fullName,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return taskService.getTasksForUser(new UserContext(userId, email, fullName), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single task by ID")
    public TaskResponse getTask(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Name") String fullName) {

        return taskService.getTask(id, new UserContext(userId, email, fullName));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new task")
    public TaskResponse createTask(
            @Valid @RequestBody TaskRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Name") String fullName) {

        return taskService.createTask(request, new UserContext(userId, email, fullName));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task (owner only)")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Name") String fullName) {

        return taskService.updateTask(id, request, new UserContext(userId, email, fullName));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task (owner only)")
    public void deleteTask(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Name") String fullName) {

        taskService.deleteTask(id, new UserContext(userId, email, fullName));
    }
}
