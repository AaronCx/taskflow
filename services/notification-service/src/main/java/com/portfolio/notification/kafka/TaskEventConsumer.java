package com.portfolio.notification.kafka;

import com.portfolio.common.event.TaskEvent;
import com.portfolio.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes all three task event topics and delegates to {@link NotificationService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = TaskEvent.TOPIC_CREATED,
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void onTaskCreated(ConsumerRecord<String, TaskEvent> record) {
        log.info("Received CREATED event: taskId={} topic={} partition={} offset={}",
                record.value().taskId(), record.topic(), record.partition(), record.offset());
        notificationService.handleTaskCreated(record.value());
    }

    @KafkaListener(
            topics = TaskEvent.TOPIC_UPDATED,
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void onTaskUpdated(ConsumerRecord<String, TaskEvent> record) {
        log.info("Received UPDATED event: taskId={}", record.value().taskId());
        notificationService.handleTaskUpdated(record.value());
    }

    @KafkaListener(
            topics = TaskEvent.TOPIC_DELETED,
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void onTaskDeleted(ConsumerRecord<String, TaskEvent> record) {
        log.info("Received DELETED event: taskId={}", record.value().taskId());
        notificationService.handleTaskDeleted(record.value());
    }
}
