package com.bankingmanagement.paymentservice.event;

import com.bankingmanagement.paymentservice.config.KafkaConfig;
import com.bankingmanagement.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes payment-related events to Kafka.
 * 
 * This component is responsible for:
 * - Converting domain entities to events
 * - Publishing events with proper keys for partitioning
 * - Handling publish failures gracefully
 * 
 * Events published:
 * - PAYMENT_INITIATED: When a new payment is created
 * - PAYMENT_PROCESSING: When payment processing starts
 * - PAYMENT_COMPLETED: When a payment is successfully completed
 * - PAYMENT_FAILED: When a payment fails
 * - PAYMENT_CANCELLED: When a payment is cancelled
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    private static final String EVENT_VERSION = "1.0";

    /**
     * Publish a PAYMENT_INITIATED event.
     * Called when a new payment is created.
     */
    public void publishPaymentInitiated(Payment payment) {
        PaymentEvent event = buildEvent(payment, PaymentEvent.PAYMENT_INITIATED);
        publishEvent(event);
    }

    /**
     * Publish a PAYMENT_PROCESSING event.
     * Called when payment processing starts.
     */
    public void publishPaymentProcessing(Payment payment) {
        PaymentEvent event = buildEvent(payment, PaymentEvent.PAYMENT_PROCESSING);
        publishEvent(event);
    }

    /**
     * Publish a PAYMENT_COMPLETED event.
     * Called when a payment is successfully completed.
     */
    public void publishPaymentCompleted(Payment payment) {
        PaymentEvent event = buildEvent(payment, PaymentEvent.PAYMENT_COMPLETED);
        publishEvent(event);
    }

    /**
     * Publish a PAYMENT_FAILED event.
     * Called when a payment fails.
     */
    public void publishPaymentFailed(Payment payment, String failureReason) {
        PaymentEvent event = buildEvent(payment, PaymentEvent.PAYMENT_FAILED);
        event.setFailureReason(failureReason);
        publishEvent(event);
    }

    /**
     * Publish a PAYMENT_CANCELLED event.
     * Called when a payment is cancelled by the user.
     */
    public void publishPaymentCancelled(Payment payment) {
        PaymentEvent event = buildEvent(payment, PaymentEvent.PAYMENT_CANCELLED);
        publishEvent(event);
    }

    /**
     * Build a PaymentEvent from a Payment entity.
     */
    private PaymentEvent buildEvent(Payment payment, String eventType) {
        return PaymentEvent.builder()
                // Event metadata
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTimestamp(Instant.now())
                .eventVersion(EVENT_VERSION)
                // Payment data
                .paymentId(payment.getId().toString())
                .referenceNumber(payment.getReferenceNumber())
                .sourceAccountId(payment.getSourceAccountId().toString())
                .destinationAccountId(payment.getDestinationAccountId().toString())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentType(payment.getType().name())
                .paymentStatus(payment.getStatus().name())
                // Related entities
                .reservationId(payment.getReservationId())
                .transactionId(payment.getTransactionId())
                .idempotencyKey(payment.getIdempotencyKey())
                // Additional context
                .description(payment.getDescription())
                .failureReason(payment.getFailureReason())
                // Timestamps
                .createdAt(payment.getCreatedAt())
                .processedAt(payment.getProcessedAt())
                .build();
    }

    /**
     * Publish event to Kafka with the payment ID as the key.
     * Using payment ID as key ensures all events for the same payment
     * go to the same partition, maintaining order.
     */
    private void publishEvent(PaymentEvent event) {
        String key = event.getPaymentId();
        String topic = KafkaConfig.PAYMENT_EVENTS_TOPIC;

        log.info("Publishing {} event: paymentId={}, reference={}, status={}", 
                event.getEventType(), event.getPaymentId(), 
                event.getReferenceNumber(), event.getPaymentStatus());

        CompletableFuture<SendResult<String, PaymentEvent>> future = 
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully: topic={}, partition={}, offset={}, eventType={}, paymentId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getEventType(),
                        event.getPaymentId());
            } else {
                log.error("Failed to publish event: eventType={}, paymentId={}, error={}",
                        event.getEventType(), event.getPaymentId(), ex.getMessage(), ex);
                // In production, you might want to:
                // - Retry with backoff
                // - Store in a dead letter queue
                // - Alert operations team
            }
        });
    }
}
