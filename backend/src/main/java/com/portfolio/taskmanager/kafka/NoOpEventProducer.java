package com.portfolio.taskmanager.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op event producer used when Kafka is disabled ({@code app.kafka.enabled=false}).
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
@Slf4j
public class NoOpEventProducer implements EventProducer {

    @Override
    public void publish(String topic, TaskEvent event) {
        log.debug("Kafka disabled — skipping event: topic={}, taskId={}", topic, event.taskId());
    }
}
