package com.portfolio.task.kafka;

import com.portfolio.common.event.TaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget Kafka producer.
 * Task operations succeed even when Kafka is temporarily unavailable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventProducer {

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;

    public void publish(String topic, TaskEvent event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(event.taskId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish {} event for task {}: {}",
                                    event.eventType(), event.taskId(), ex.getMessage());
                        } else {
                            log.debug("Published {} to topic={} partition={} offset={}",
                                    event.eventType(), topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            log.warn("Kafka unavailable — {} event for task {} dropped: {}",
                    event.eventType(), event.taskId(), ex.getMessage());
        }
    }
}
