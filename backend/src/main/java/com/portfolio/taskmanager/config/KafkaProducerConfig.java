package com.portfolio.taskmanager.config;

import com.portfolio.taskmanager.kafka.TaskEvent;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Kafka producer configuration and topic auto-creation.
 *
 * Topics are created by KafkaAdmin on startup if they don't already exist.
 * Producer uses JSON serialisation for the event payload.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Producer factory ──────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, TaskEvent> taskEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,       bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,    StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,  JsonSerializer.class,
                // Reduce blocking time when Kafka is unavailable
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,      "5000",
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,     "10000",
                ProducerConfig.MAX_BLOCK_MS_CONFIG,            "5000",
                // Idempotent producer — prevents duplicate messages on retries
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,      true,
                ProducerConfig.ACKS_CONFIG,                    "all"
        ));
    }

    @Bean
    public KafkaTemplate<String, TaskEvent> kafkaTemplate() {
        return new KafkaTemplate<>(taskEventProducerFactory());
    }

    // ── Topic definitions (auto-created by KafkaAdmin) ────────────────

    @Bean
    public NewTopic taskCreatedTopic() {
        return TopicBuilder.name("task.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskUpdatedTopic() {
        return TopicBuilder.name("task.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskDeletedTopic() {
        return TopicBuilder.name("task.deleted")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
