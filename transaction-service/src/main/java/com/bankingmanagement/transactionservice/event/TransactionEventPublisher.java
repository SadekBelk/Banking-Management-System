package com.bankingmanagement.transactionservice.event;

import com.bankingmanagement.transactionservice.config.KafkaConfig;
import com.bankingmanagement.transactionservice.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes transaction-related events to Kafka.
 * 
 * This component is responsible for:
 * - Converting domain entities to events
 * - Publishing events with proper keys for partitioning
 * - Handling publish failures gracefully
 * 
 * Events published:
 * - TRANSACTION_CREATED: When a new transaction is recorded in the ledger
 * - TRANSACTION_COMPLETED: When a transaction is marked as successful
 * - TRANSACTION_FAILED: When a transaction fails
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String EVENT_VERSION = "1.0";

    /**
     * Publish a TRANSACTION_CREATED event.
     * Called when a new transaction is recorded in the ledger.
     */
    public void publishTransactionCreated(Transaction transaction) {
        TransactionEvent event = buildEvent(transaction, TransactionEvent.TRANSACTION_CREATED);
        publishEvent(event);
    }

    /**
     * Publish a TRANSACTION_COMPLETED event.
     * Called when a transaction is successfully completed.
     */
    public void publishTransactionCompleted(Transaction transaction) {
        TransactionEvent event = buildEvent(transaction, TransactionEvent.TRANSACTION_COMPLETED);
        publishEvent(event);
    }

    /**
     * Publish a TRANSACTION_FAILED event.
     * Called when a transaction fails.
     */
    public void publishTransactionFailed(Transaction transaction, String failureReason) {
        TransactionEvent event = buildEvent(transaction, TransactionEvent.TRANSACTION_FAILED);
        event.setFailureReason(failureReason);
        publishEvent(event);
    }

    /**
     * Build a TransactionEvent from a Transaction entity.
     */
    private TransactionEvent buildEvent(Transaction transaction, String eventType) {
        return TransactionEvent.builder()
                // Event metadata
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTimestamp(Instant.now())
                .eventVersion(EVENT_VERSION)
                // Transaction data
                .transactionId(transaction.getId().toString())
                .referenceNumber(transaction.getReferenceNumber())
                .sourceAccountId(transaction.getSourceAccountId().toString())
                .destinationAccountId(transaction.getDestinationAccountId().toString())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionType(transaction.getType().name())
                .transactionStatus(transaction.getStatus().name())
                // Related entities
                .paymentId(transaction.getPaymentId() != null ? transaction.getPaymentId().toString() : null)
                .reservationId(transaction.getReservationId() != null ? transaction.getReservationId().toString() : null)
                // Additional context
                .description(transaction.getDescription())
                .failureReason(transaction.getFailureReason())
                .build();
    }

    /**
     * Publish event to Kafka with the transaction ID as the key.
     * Using transaction ID as key ensures all events for the same transaction
     * go to the same partition, maintaining order.
     */
    private void publishEvent(TransactionEvent event) {
        String key = event.getTransactionId();
        String topic = KafkaConfig.TRANSACTION_EVENTS_TOPIC;

        log.info("Publishing {} event: transactionId={}, reference={}", 
                event.getEventType(), event.getTransactionId(), event.getReferenceNumber());

        CompletableFuture<SendResult<String, TransactionEvent>> future = 
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully: topic={}, partition={}, offset={}, eventType={}, transactionId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getEventType(),
                        event.getTransactionId());
            } else {
                log.error("Failed to publish event: eventType={}, transactionId={}, error={}",
                        event.getEventType(), event.getTransactionId(), ex.getMessage(), ex);
                // In production, you might want to:
                // - Retry with backoff
                // - Store in a dead letter queue
                // - Alert operations team
            }
        });
    }
}
