package com.portfolio.taskmanager.controller;

import com.portfolio.taskmanager.dto.request.TaskRequest;
import com.portfolio.taskmanager.dto.response.TaskResponse;
import com.portfolio.taskmanager.entity.User;
import com.portfolio.taskmanager.enums.TaskStatus;
import com.portfolio.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Task CRUD endpoints — all require a valid JWT.
 *
 * Users can only see and manage their own tasks.
 * The authenticated principal is injected via {@code @AuthenticationPrincipal}.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Create, read, update and delete tasks")
public class TaskController {

    private final TaskService taskService;

    // ── GET /api/tasks ────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all tasks for the authenticated user",
               description = "Optionally filter by status and/or search by title/description")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @Parameter(description = "Optional status filter: TODO | IN_PROGRESS | DONE")
            @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Search tasks by title or description")
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(taskService.getTasksForUser(currentUser, status, search));
    }

    // ── GET /api/tasks/{id} ───────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get a single task by ID")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(taskService.getTaskById(id, currentUser));
    }

    // ── POST /api/tasks ───────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(request, currentUser));
    }

    // ── PUT /api/tasks/{id} ───────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing task (full replacement)")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(taskService.updateTask(id, request, currentUser));
    }

    // ── DELETE /api/tasks/{id} ────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        taskService.deleteTask(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ── Bulk operations ─────────────────────────────────────────────

    @PutMapping("/bulk-update")
    @Operation(summary = "Update status for multiple tasks")
    public ResponseEntity<Map<String, Integer>> bulkUpdateStatus(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {

        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String status = (String) body.get("status");
        TaskStatus newStatus = TaskStatus.valueOf(status);

        int updated = taskService.bulkUpdateStatus(
                ids.stream().map(Long::valueOf).toList(), newStatus, currentUser);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @DeleteMapping("/bulk-delete")
    @Operation(summary = "Delete multiple tasks")
    public ResponseEntity<Map<String, Integer>> bulkDelete(
            @RequestBody Map<String, List<Integer>> body,
            @AuthenticationPrincipal User currentUser) {

        List<Long> ids = body.get("ids").stream().map(Long::valueOf).toList();
        int deleted = taskService.bulkDelete(ids, currentUser);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
