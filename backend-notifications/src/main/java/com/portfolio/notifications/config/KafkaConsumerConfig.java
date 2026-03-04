package com.portfolio.notifications.config;

import com.portfolio.notifications.kafka.TaskEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Kafka consumer configuration for the notification service.
 *
 * Key design decisions:
 *  - Consumer group "notification-service" — gives each instance its own offset tracking
 *  - {@code ErrorHandlingDeserializer} wraps {@code JsonDeserializer} so a single
 *    malformed message is skipped rather than stalling the entire partition
 *  - {@code DefaultErrorHandler} retries failed processing 3 times (2 s apart),
 *    then logs and skips to prevent consumer lag accumulation
 *  - {@code auto.offset.reset=earliest} so no events are missed when the
 *    service starts after the producer
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, TaskEvent> consumerFactory() {
        JsonDeserializer<TaskEvent> jsonDeserializer = new JsonDeserializer<>(TaskEvent.class);
        jsonDeserializer.addTrustedPackages("com.portfolio.taskmanager.kafka",
                                            "com.portfolio.notifications.kafka");
        jsonDeserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,       bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG,                "notification-service",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,       "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,      false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,  StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName()
        ), new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TaskEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TaskEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        // Process up to 3 partitions concurrently
        factory.setConcurrency(3);
        return factory;
    }

    /**
     * Retry failed messages 3 times with a 2-second fixed backoff,
     * then skip (log-and-continue) to avoid blocking the partition.
     */
    @Bean
    public CommonErrorHandler errorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new FixedBackOff(2_000L, 3L));
        handler.setCommitRecovered(true);
        return handler;
    }
}
