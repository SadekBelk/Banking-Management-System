package com.bankingmanagement.paymentservice.grpc;

import com.banking.proto.common.Money;
import com.banking.proto.transaction.*;
import com.bankingmanagement.paymentservice.exception.TransactionException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * gRPC Client for communicating with transaction-service.
 * 
 * This is the CLIENT side - payment-service calls transaction-service's gRPC server.
 * 
 * ARCHITECTURAL RULES:
 * - payment-service is the ORCHESTRATOR (gRPC CLIENT)
 * - transaction-service is the LEDGER OWNER (gRPC SERVER)
 * - All transaction recording goes through this client
 * - Errors are translated from gRPC status codes to domain exceptions
 */
@Component
@Slf4j
public class TransactionGrpcClient {

    @GrpcClient("transaction-service")
    private TransactionServiceGrpc.TransactionServiceBlockingStub transactionServiceStub;

    /**
     * Create a new transaction record in the ledger.
     * 
     * @param sourceAccountId Source account UUID
     * @param destinationAccountId Destination account UUID
     * @param amount Transaction amount
     * @param currency Currency code (e.g., "USD")
     * @param type Transaction type
     * @param paymentId Reference to payment
     * @param reservationId Reference to account reservation
     * @param description Optional description
     * @param idempotencyKey Unique key for idempotency
     * @return Transaction ID from transaction-service
     */
    public CreateTransactionResult createTransaction(
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            TransactionType type,
            UUID paymentId,
            String reservationId,
            String description,
            String idempotencyKey) {
        
        log.info("gRPC Client: CreateTransaction sourceAccount={}, destAccount={}, amount={} {}", 
                sourceAccountId, destinationAccountId, amount, currency);

        try {
            CreateTransactionRequest request = CreateTransactionRequest.newBuilder()
                    .setSourceAccountId(sourceAccountId.toString())
                    .setDestinationAccountId(destinationAccountId.toString())
                    .setAmount(Money.newBuilder()
                            .setAmount(amount.longValue())
                            .setCurrency(currency)
                            .build())
                    .setType(type)
                    .setPaymentId(paymentId != null ? paymentId.toString() : "")
                    .setReservationId(reservationId != null ? reservationId : "")
                    .setDescription(description != null ? description : "")
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            CreateTransactionResponse response = transactionServiceStub.createTransaction(request);
            
            log.info("gRPC Client: CreateTransaction success - transactionId={}, reference={}", 
                    response.getTransactionId(), response.getReferenceNumber());
            
            return new CreateTransactionResult(
                    response.getTransactionId(),
                    response.getReferenceNumber()
            );

        } catch (StatusRuntimeException e) {
            handleGrpcError("CreateTransaction", e);
            throw e;
        }
    }

    /**
     * Mark a transaction as completed.
     * 
     * @param transactionId Transaction ID to complete
     */
    public void completeTransaction(String transactionId) {
        log.info("gRPC Client: CompleteTransaction transactionId={}", transactionId);

        try {
            CompleteTransactionRequest request = CompleteTransactionRequest.newBuilder()
                    .setTransactionId(transactionId)
                    .build();

            transactionServiceStub.completeTransaction(request);
            
            log.info("gRPC Client: CompleteTransaction success");

        } catch (StatusRuntimeException e) {
            handleGrpcError("CompleteTransaction", e);
            throw e;
        }
    }

    /**
     * Mark a transaction as failed.
     * 
     * @param transactionId Transaction ID to fail
     * @param reason Why it failed
     */
    public void failTransaction(String transactionId, String reason) {
        log.info("gRPC Client: FailTransaction transactionId={}, reason={}", transactionId, reason);

        try {
            FailTransactionRequest request = FailTransactionRequest.newBuilder()
                    .setTransactionId(transactionId)
                    .setFailureReason(reason)
                    .build();

            transactionServiceStub.failTransaction(request);
            
            log.info("gRPC Client: FailTransaction success");

        } catch (StatusRuntimeException e) {
            handleGrpcError("FailTransaction", e);
            throw e;
        }
    }

    /**
     * Get transaction details.
     * 
     * @param transactionId Transaction ID
     * @return Transaction details
     */
    public GetTransactionResponse getTransaction(String transactionId) {
        log.info("gRPC Client: GetTransaction transactionId={}", transactionId);

        try {
            GetTransactionRequest request = GetTransactionRequest.newBuilder()
                    .setTransactionId(transactionId)
                    .build();

            GetTransactionResponse response = transactionServiceStub.getTransaction(request);
            
            log.info("gRPC Client: GetTransaction success - status={}", response.getStatus());
            return response;

        } catch (StatusRuntimeException e) {
            handleGrpcError("GetTransaction", e);
            throw e;
        }
    }

    /**
     * Translates gRPC status codes to domain exceptions.
     */
    private void handleGrpcError(String operation, StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();
        String description = e.getStatus().getDescription();

        log.error("gRPC Client: {} failed - code={}, description={}", operation, code, description);

        switch (code) {
            case NOT_FOUND -> throw new TransactionException(
                    description != null ? description : "Transaction not found"
            );
            case FAILED_PRECONDITION -> throw new TransactionException(
                    description != null ? description : "Invalid transaction state"
            );
            case INVALID_ARGUMENT -> throw new IllegalArgumentException(
                    description != null ? description : "Invalid argument"
            );
            case UNAVAILABLE -> throw new RuntimeException(
                    "Transaction service is unavailable. Please try again later."
            );
            case DEADLINE_EXCEEDED -> throw new RuntimeException(
                    "Transaction service request timed out. Please try again."
            );
            default -> throw new RuntimeException(
                    "Transaction service error: " + code + " - " + description
            );
        }
    }

    /**
     * Result of creating a transaction.
     */
    public record CreateTransactionResult(String transactionId, String referenceNumber) {}
}
