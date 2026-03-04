package com.portfolio.notification.kafka;

import com.portfolio.common.event.TaskEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, TaskEvent> consumerFactory() {
        JsonDeserializer<TaskEvent> jsonDeserializer = new JsonDeserializer<>(TaskEvent.class, false);
        // Trust the package where TaskEvent is defined
        jsonDeserializer.addTrustedPackages("com.portfolio.common.event");

        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, "notification-service",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class,
                        JsonDeserializer.TRUSTED_PACKAGES, "com.portfolio.common.event"
                ),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TaskEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TaskEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);

        // Retry 3 times with 2s back-off before skipping the record
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(2000L, 3L));
        errorHandler.setCommitRecovered(true);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
