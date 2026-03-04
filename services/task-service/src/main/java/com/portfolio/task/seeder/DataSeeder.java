package com.portfolio.task.seeder;

import com.portfolio.task.entity.Priority;
import com.portfolio.task.entity.Task;
import com.portfolio.task.entity.TaskStatus;
import com.portfolio.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds sample tasks for the two demo users (IDs 1 and 2 from auth-service).
 * <p>
 * Uses the well-known demo user IDs — in a production system this would be
 * replaced by a more robust seeding strategy (e.g., calling the auth-service).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final TaskRepository taskRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (taskRepository.count() > 0) {
            return;
        }

        // IDs 1 and 2 correspond to alice@demo.com and bob@demo.com seeded by auth-service
        long aliceId = 1L;
        long bobId   = 2L;

        List<Task> tasks = List.of(
            task("Set up CI/CD pipeline",   "Configure GitHub Actions for build + test + deploy",
                 TaskStatus.DONE,        Priority.HIGH,     aliceId, "alice@demo.com", "Alice Demo", null, null),
            task("Design database schema",  "ERD for tasks, users, notifications tables",
                 TaskStatus.DONE,        Priority.HIGH,     aliceId, "alice@demo.com", "Alice Demo", null, null),
            task("Implement JWT auth",      "Login, register, refresh token flow",
                 TaskStatus.IN_PROGRESS, Priority.CRITICAL, aliceId, "alice@demo.com", "Alice Demo", bobId, "Bob Demo"),
            task("Build task CRUD API",     "REST endpoints with pagination and filtering",
                 TaskStatus.IN_PROGRESS, Priority.HIGH,     aliceId, "alice@demo.com", "Alice Demo", null, null),
            task("Add Swagger docs",        "Annotate all controllers with OpenAPI 3 spec",
                 TaskStatus.TODO,        Priority.MEDIUM,   aliceId, "alice@demo.com", "Alice Demo", null, null),
            task("Create React frontend",   "Login, Dashboard, Task Detail pages",
                 TaskStatus.IN_REVIEW,   Priority.HIGH,     bobId,   "bob@demo.com",   "Bob Demo",   aliceId, "Alice Demo"),
            task("Write unit tests",        "80%+ coverage for service and controller layers",
                 TaskStatus.TODO,        Priority.MEDIUM,   bobId,   "bob@demo.com",   "Bob Demo",   null, null),
            task("Add Kafka events",        "Publish task.created/updated/deleted to Kafka",
                 TaskStatus.TODO,        Priority.HIGH,     bobId,   "bob@demo.com",   "Bob Demo",   null, null),
            task("Deploy to cloud",         "Containerize and deploy to AWS ECS or GCP Cloud Run",
                 TaskStatus.TODO,        Priority.MEDIUM,   bobId,   "bob@demo.com",   "Bob Demo",   null, null),
            task("Performance testing",     "Load test API with k6, target p99 < 200ms",
                 TaskStatus.TODO,        Priority.LOW,      aliceId, "alice@demo.com", "Alice Demo", null, null)
        );

        taskRepository.saveAll(tasks);
        log.info("Seeded {} demo tasks", tasks.size());
    }

    private Task task(String title, String desc, TaskStatus status, Priority priority,
                      Long ownerId, String ownerEmail, String ownerName,
                      Long assignedToId, String assignedToName) {
        return Task.builder()
                .title(title).description(desc)
                .status(status).priority(priority)
                .dueDate(LocalDateTime.now().plusDays(7))
                .ownerId(ownerId).ownerEmail(ownerEmail).ownerName(ownerName)
                .assignedToId(assignedToId).assignedToName(assignedToName)
                .build();
    }
}
