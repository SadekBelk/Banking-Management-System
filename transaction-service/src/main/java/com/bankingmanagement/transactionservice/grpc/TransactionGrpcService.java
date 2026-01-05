package com.bankingmanagement.transactionservice.grpc;

import com.banking.proto.common.Money;
import com.banking.proto.transaction.*;
import com.bankingmanagement.transactionservice.event.TransactionEventPublisher;
import com.bankingmanagement.transactionservice.model.Transaction;
import com.bankingmanagement.transactionservice.model.TransactionStatus;
import com.bankingmanagement.transactionservice.model.TransactionType;
import com.bankingmanagement.transactionservice.repository.TransactionRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * gRPC Server implementation for TransactionService.
 * 
 * This service is the LEDGER OWNER - it maintains the immutable audit trail
 * of all transactions in the banking system.
 * 
 * ARCHITECTURAL RULES:
 * - transaction-service is the gRPC SERVER (it owns the ledger)
 * - payment-service is the gRPC CLIENT (it orchestrates)
 * - This service does NOT modify account balances - only records transactions
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher eventPublisher;

    /**
     * Create a new transaction in the ledger.
     * Idempotent: same idempotency_key returns existing transaction.
     */
    @Override
    @Transactional
    public void createTransaction(CreateTransactionRequest request, 
                                   StreamObserver<CreateTransactionResponse> responseObserver) {
        log.info("gRPC CreateTransaction: sourceAccount={}, destAccount={}, amount={} {}, paymentId={}", 
                request.getSourceAccountId(), 
                request.getDestinationAccountId(),
                request.getAmount().getAmount(),
                request.getAmount().getCurrency(),
                request.getPaymentId());

        try {
            // 1️⃣ Validate request
            validateCreateRequest(request);

            // 2️⃣ Check idempotency - return existing transaction if found
            Optional<Transaction> existing = transactionRepository
                    .findByIdempotencyKey(request.getIdempotencyKey());
            
            if (existing.isPresent()) {
                Transaction tx = existing.get();
                log.info("gRPC CreateTransaction: Idempotent hit, returning existing transaction={}", tx.getId());
                
                responseObserver.onNext(buildCreateResponse(tx));
                responseObserver.onCompleted();
                return;
            }

            // 3️⃣ Create new transaction
            Transaction transaction = Transaction.builder()
                    .referenceNumber(generateReferenceNumber())
                    .sourceAccountId(UUID.fromString(request.getSourceAccountId()))
                    .destinationAccountId(UUID.fromString(request.getDestinationAccountId()))
                    .amount(BigDecimal.valueOf(request.getAmount().getAmount()))
                    .currency(request.getAmount().getCurrency())
                    .type(mapTransactionType(request.getType()))
                    .status(TransactionStatus.PENDING)
                    .paymentId(request.getPaymentId().isEmpty() ? null : UUID.fromString(request.getPaymentId()))
                    .reservationId(request.getReservationId().isEmpty() ? null : UUID.fromString(request.getReservationId()))
                    .description(request.getDescription())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();

            Transaction saved = transactionRepository.save(transaction);
            log.info("gRPC CreateTransaction: Created transaction={}, reference={}", 
                    saved.getId(), saved.getReferenceNumber());

            // 5️⃣ Publish Kafka event
            eventPublisher.publishTransactionCreated(saved);

            // 6️⃣ Return response
            responseObserver.onNext(buildCreateResponse(saved));
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("gRPC CreateTransaction failed - invalid argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC CreateTransaction unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error creating transaction")
                    .asRuntimeException());
        }
    }

    /**
     * Mark a transaction as completed.
     * Called after funds have been successfully transferred.
     */
    @Override
    @Transactional
    public void completeTransaction(CompleteTransactionRequest request, 
                                     StreamObserver<CompleteTransactionResponse> responseObserver) {
        log.info("gRPC CompleteTransaction: transactionId={}", request.getTransactionId());

        try {
            UUID transactionId = parseUUID(request.getTransactionId(), "transaction_id");

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Transaction not found: " + transactionId));

            // Validate state transition
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                throw new IllegalStateException(
                        "Cannot complete transaction with status: " + transaction.getStatus());
            }

            // Update status
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(Instant.now());
            transactionRepository.save(transaction);

            // Publish Kafka event
            eventPublisher.publishTransactionCompleted(transaction);

            log.info("gRPC CompleteTransaction: Transaction {} marked COMPLETED", transactionId);

            responseObserver.onNext(CompleteTransactionResponse.newBuilder()
                    .setStatus(com.banking.proto.transaction.TransactionStatus.COMPLETED)
                    .build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("gRPC CompleteTransaction failed - invalid argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (IllegalStateException e) {
            log.warn("gRPC CompleteTransaction failed - invalid state: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC CompleteTransaction unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    /**
     * Mark a transaction as failed.
     * Called when the payment flow encounters an error.
     */
    @Override
    @Transactional
    public void failTransaction(FailTransactionRequest request, 
                                 StreamObserver<FailTransactionResponse> responseObserver) {
        log.info("gRPC FailTransaction: transactionId={}, reason={}", 
                request.getTransactionId(), request.getFailureReason());

        try {
            UUID transactionId = parseUUID(request.getTransactionId(), "transaction_id");

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Transaction not found: " + transactionId));

            // Validate state transition
            if (transaction.getStatus() == TransactionStatus.COMPLETED) {
                throw new IllegalStateException("Cannot fail a completed transaction");
            }

            // Update status
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(request.getFailureReason());
            transactionRepository.save(transaction);

            // Publish Kafka event
            eventPublisher.publishTransactionFailed(transaction, request.getFailureReason());

            log.info("gRPC FailTransaction: Transaction {} marked FAILED", transactionId);

            responseObserver.onNext(FailTransactionResponse.newBuilder()
                    .setStatus(com.banking.proto.transaction.TransactionStatus.FAILED)
                    .build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("gRPC FailTransaction failed - invalid argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (IllegalStateException e) {
            log.warn("gRPC FailTransaction failed - invalid state: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC FailTransaction unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    /**
     * Get a transaction by ID.
     */
    @Override
    public void getTransaction(GetTransactionRequest request, 
                                StreamObserver<GetTransactionResponse> responseObserver) {
        log.info("gRPC GetTransaction: transactionId={}", request.getTransactionId());

        try {
            UUID transactionId = parseUUID(request.getTransactionId(), "transaction_id");

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Transaction not found: " + transactionId));

            responseObserver.onNext(buildGetResponse(transaction));
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("gRPC GetTransaction failed: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC GetTransaction unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    // ==================== Helper Methods ====================

    private void validateCreateRequest(CreateTransactionRequest request) {
        if (request.getSourceAccountId() == null || request.getSourceAccountId().isBlank()) {
            throw new IllegalArgumentException("source_account_id is required");
        }
        if (request.getDestinationAccountId() == null || request.getDestinationAccountId().isBlank()) {
            throw new IllegalArgumentException("destination_account_id is required");
        }
        if (request.getAmount() == null || request.getAmount().getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotency_key is required");
        }
        // Validate UUIDs
        parseUUID(request.getSourceAccountId(), "source_account_id");
        parseUUID(request.getDestinationAccountId(), "destination_account_id");
    }

    private UUID parseUUID(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID: " + value);
        }
    }

    private String generateReferenceNumber() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TransactionType mapTransactionType(com.banking.proto.transaction.TransactionType protoType) {
        return switch (protoType) {
            case TRANSFER -> TransactionType.TRANSFER;
            case DEPOSIT -> TransactionType.DEPOSIT;
            case WITHDRAWAL -> TransactionType.WITHDRAWAL;
            case BILL_PAYMENT -> TransactionType.BILL_PAYMENT;
            case P2P -> TransactionType.P2P;
            case REFUND -> TransactionType.REFUND;
            default -> TransactionType.TRANSFER;
        };
    }

    private com.banking.proto.transaction.TransactionType mapToProtoType(TransactionType type) {
        return switch (type) {
            case TRANSFER -> com.banking.proto.transaction.TransactionType.TRANSFER;
            case DEPOSIT -> com.banking.proto.transaction.TransactionType.DEPOSIT;
            case WITHDRAWAL -> com.banking.proto.transaction.TransactionType.WITHDRAWAL;
            case BILL_PAYMENT -> com.banking.proto.transaction.TransactionType.BILL_PAYMENT;
            case P2P -> com.banking.proto.transaction.TransactionType.P2P;
            case REFUND -> com.banking.proto.transaction.TransactionType.REFUND;
        };
    }

    private com.banking.proto.transaction.TransactionStatus mapToProtoStatus(TransactionStatus status) {
        return switch (status) {
            case PENDING -> com.banking.proto.transaction.TransactionStatus.PENDING;
            case COMPLETED -> com.banking.proto.transaction.TransactionStatus.COMPLETED;
            case FAILED -> com.banking.proto.transaction.TransactionStatus.FAILED;
        };
    }

    private CreateTransactionResponse buildCreateResponse(Transaction tx) {
        return CreateTransactionResponse.newBuilder()
                .setTransactionId(tx.getId().toString())
                .setReferenceNumber(tx.getReferenceNumber())
                .setStatus(mapToProtoStatus(tx.getStatus()))
                .build();
    }

    private GetTransactionResponse buildGetResponse(Transaction tx) {
        GetTransactionResponse.Builder builder = GetTransactionResponse.newBuilder()
                .setTransactionId(tx.getId().toString())
                .setReferenceNumber(tx.getReferenceNumber())
                .setSourceAccountId(tx.getSourceAccountId().toString())
                .setDestinationAccountId(tx.getDestinationAccountId().toString())
                .setAmount(Money.newBuilder()
                        .setAmount(tx.getAmount().longValue())
                        .setCurrency(tx.getCurrency())
                        .build())
                .setType(mapToProtoType(tx.getType()))
                .setStatus(mapToProtoStatus(tx.getStatus()))
                .setCreatedAt(tx.getCreatedAt().toEpochMilli())
                .setUpdatedAt(tx.getUpdatedAt().toEpochMilli());

        if (tx.getPaymentId() != null) {
            builder.setPaymentId(tx.getPaymentId().toString());
        }
        if (tx.getDescription() != null) {
            builder.setDescription(tx.getDescription());
        }
        if (tx.getFailureReason() != null) {
            builder.setFailureReason(tx.getFailureReason());
        }

        return builder.build();
    }
}
