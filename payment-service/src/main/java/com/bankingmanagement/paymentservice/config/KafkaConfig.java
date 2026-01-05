package com.bankingmanagement.paymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for payment-service.
 * 
 * This service is a PRODUCER only - it publishes payment events.
 * Topics are created automatically if they don't exist.
 */
@Configuration
public class KafkaConfig {

    public static final String PAYMENT_EVENTS_TOPIC = "banking.payments.events";

    @Value("${spring.kafka.topic.partitions:3}")
    private int partitions;

    @Value("${spring.kafka.topic.replication-factor:1}")
    private int replicationFactor;

    /**
     * Creates the payment events topic.
     * 
     * Partitioning strategy: Events are partitioned by paymentId
     * to ensure ordering per payment.
     */
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PAYMENT_EVENTS_TOPIC)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config("retention.ms", "604800000") // 7 days retention
                .config("cleanup.policy", "delete")
                .build();
    }
}
