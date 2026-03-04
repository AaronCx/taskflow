package com.portfolio.notifications.repository;

import com.portfolio.notifications.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Most-recent N notifications for a user, regardless of read state. */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Count of unread notifications for a user (used for the badge). */
    long countByUserIdAndReadFalse(Long userId);

    /** Mark all of a user's notifications as read in a single UPDATE. */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllReadByUserId(Long userId);
}
