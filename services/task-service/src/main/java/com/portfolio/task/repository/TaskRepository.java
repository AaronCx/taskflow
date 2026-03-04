package com.portfolio.task.repository;

import com.portfolio.task.entity.Task;
import com.portfolio.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** All tasks visible to a user (owned or assigned). */
    Page<Task> findByOwnerIdOrAssignedToId(Long ownerId, Long assignedToId, Pageable pageable);

    /** Tasks by owner filtered by status. */
    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);

    /** Ensure task belongs to the requesting user before mutation. */
    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}
