package com.portfolio.task.kafka;

import com.portfolio.common.event.TaskEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, TaskEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                // Idempotent producer — prevents duplicate messages on retry
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.RETRIES_CONFIG, 3,
                // Don't block task operations longer than 5 s when broker is down
                ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000
        ));
    }

    @Bean
    public KafkaTemplate<String, TaskEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Auto-create topics (3 partitions, replication factor 1 for local dev) ──

    @Bean public NewTopic topicCreated()  { return new NewTopic(TaskEvent.TOPIC_CREATED,  3, (short) 1); }
    @Bean public NewTopic topicUpdated()  { return new NewTopic(TaskEvent.TOPIC_UPDATED,  3, (short) 1); }
    @Bean public NewTopic topicDeleted()  { return new NewTopic(TaskEvent.TOPIC_DELETED,  3, (short) 1); }
}
