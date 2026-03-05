package com.portfolio.taskmanager.service;

import com.portfolio.taskmanager.dto.request.TaskRequest;
import com.portfolio.taskmanager.dto.response.TaskResponse;
import com.portfolio.taskmanager.entity.Task;
import com.portfolio.taskmanager.entity.User;
import com.portfolio.taskmanager.enums.TaskPriority;
import com.portfolio.taskmanager.enums.TaskStatus;
import com.portfolio.taskmanager.exception.ResourceNotFoundException;
import com.portfolio.taskmanager.kafka.TaskEventProducer;
import com.portfolio.taskmanager.repository.TaskRepository;
import com.portfolio.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock TaskRepository       taskRepository;
    @Mock UserRepository       userRepository;
    // TaskEventProducer was added in the Kafka branch — must be mocked so
    // @InjectMocks doesn't leave it null and cause a NullPointerException.
    @Mock TaskEventProducer    eventProducer;

    @InjectMocks TaskService taskService;

    private User  owner;
    private Task  sampleTask;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).firstName("Alice").lastName("J")
                    .email("alice@test.com").password("hashed").build();

        sampleTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("A test")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(3))
                .owner(owner)
                .build();
    }

    @Test
    @DisplayName("getTasksForUser: returns mapped DTOs ordered by creation")
    void getTasksForUser_returnsList() {
        when(taskRepository.findByOwnerOrderByCreatedAtDesc(owner))
                .thenReturn(List.of(sampleTask));

        List<TaskResponse> result = taskService.getTasksForUser(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("createTask: saves and returns new task DTO")
    void createTask_success() {
        TaskRequest request = new TaskRequest("New Task", "Desc",
                TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().plusDays(5), null);

        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskResponse response = taskService.createTask(request, owner);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("deleteTask: throws ResourceNotFoundException when task not owned by user")
    void deleteTask_notOwned() {
        when(taskRepository.findByIdAndOwner(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(99L, owner))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getTaskById: returns DTO when task exists and is owned")
    void getTaskById_success() {
        when(taskRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(sampleTask));

        TaskResponse response = taskService.getTaskById(1L, owner);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Test Task");
    }
}
