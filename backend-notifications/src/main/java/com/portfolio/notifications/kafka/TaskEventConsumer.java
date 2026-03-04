package com.portfolio.notifications.kafka;

import com.portfolio.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer — listens on all three task event topics.
 *
 * Each method delegates immediately to {@link NotificationService}
 * to keep the listener thin and testable.
 *
 * Error handling: the {@code DefaultErrorHandler} configured in
 * {@link com.portfolio.notifications.config.KafkaConsumerConfig} will
 * retry failed messages 3 times with back-off, then log and skip them
 * to avoid consumer lag build-up.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventConsumer {

    private final NotificationService notificationService;

    // ── task.created ──────────────────────────────────────────────────

    @KafkaListener(
            topics          = TaskEvent.TOPIC_CREATED,
            groupId         = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTaskCreated(
            @Payload TaskEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC)     String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int    partition,
            @Header(KafkaHeaders.OFFSET)             long   offset) {

        log.info("[{}] TASK_CREATED  taskId={} title='{}' owner={}  partition={} offset={}",
                topic, event.taskId(), event.taskTitle(), event.ownerEmail(), partition, offset);

        notificationService.handleTaskCreated(event);
    }

    // ── task.updated ──────────────────────────────────────────────────

    @KafkaListener(
            topics          = TaskEvent.TOPIC_UPDATED,
            groupId         = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTaskUpdated(
            @Payload TaskEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC)     String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int    partition,
            @Header(KafkaHeaders.OFFSET)             long   offset) {

        log.info("[{}] TASK_UPDATED  taskId={} title='{}' {} → {} partition={} offset={}",
                topic, event.taskId(), event.taskTitle(),
                event.oldStatus(), event.newStatus(), partition, offset);

        notificationService.handleTaskUpdated(event);
    }

    // ── task.deleted ──────────────────────────────────────────────────

    @KafkaListener(
            topics          = TaskEvent.TOPIC_DELETED,
            groupId         = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTaskDeleted(
            @Payload TaskEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC)     String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int    partition,
            @Header(KafkaHeaders.OFFSET)             long   offset) {

        log.info("[{}] TASK_DELETED  taskId={} title='{}' owner={} partition={} offset={}",
                topic, event.taskId(), event.taskTitle(), event.ownerEmail(), partition, offset);

        notificationService.handleTaskDeleted(event);
    }
}
