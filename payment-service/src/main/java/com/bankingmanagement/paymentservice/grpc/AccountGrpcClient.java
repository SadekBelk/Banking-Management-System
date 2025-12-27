package com.bankingmanagement.paymentservice.grpc;

import com.banking.proto.account.*;
import com.banking.proto.common.Money;
import com.bankingmanagement.paymentservice.exception.AccountNotFoundException;
import com.bankingmanagement.paymentservice.exception.InsufficientBalanceException;
import com.bankingmanagement.paymentservice.exception.ReservationException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * gRPC Client for communicating with account-service.
 * 
 * This is the CLIENT side - payment-service calls account-service's gRPC server.
 * 
 * ARCHITECTURAL RULES:
 * - payment-service is the ORCHESTRATOR (gRPC CLIENT)
 * - account-service is the BALANCE OWNER (gRPC SERVER)
 * - All balance operations go through this client
 * - Errors are translated from gRPC status codes to domain exceptions
 */
@Component
@Slf4j
public class AccountGrpcClient {

    /**
     * The gRPC stub is injected by grpc-client-spring-boot-starter.
     * "account-service" refers to the channel name in application.yml
     */
    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountServiceStub;

    /**
     * Get available balance for an account.
     * 
     * @param accountId The account to check
     * @return Available balance (actual - reserved)
     * @throws AccountNotFoundException if account doesn't exist
     */
    public BigDecimal getAvailableBalance(UUID accountId) {
        log.info("gRPC Client: GetBalance for accountId={}", accountId);

        try {
            GetBalanceRequest request = GetBalanceRequest.newBuilder()
                    .setAccountId(accountId.toString())
                    .build();

            GetBalanceResponse response = accountServiceStub.getBalance(request);
            
            BigDecimal balance = BigDecimal.valueOf(response.getAvailableBalance().getAmount());
            String currency = response.getAvailableBalance().getCurrency();
            
            log.info("gRPC Client: GetBalance success - balance={} {}", balance, currency);
            return balance;

        } catch (StatusRuntimeException e) {
            handleGrpcError("GetBalance", e);
            throw e; // Won't reach here, handleGrpcError always throws
        }
    }

    /**
     * Reserve balance - Step 1 of payment flow.
     * Locks money without deducting it. Idempotent via idempotencyKey.
     * 
     * @param accountId The source account
     * @param amount Amount to reserve
     * @param currency Currency (e.g., "USD")
     * @param idempotencyKey Unique key for this reservation (prevents duplicates)
     * @return Reservation ID (needed for commit/release)
     * @throws AccountNotFoundException if account doesn't exist
     * @throws InsufficientBalanceException if not enough available balance
     */
    public String reserveBalance(UUID accountId, BigDecimal amount, String currency, String idempotencyKey) {
        log.info("gRPC Client: ReserveBalance accountId={}, amount={} {}, idempotencyKey={}", 
                accountId, amount, currency, idempotencyKey);

        try {
            ReserveBalanceRequest request = ReserveBalanceRequest.newBuilder()
                    .setAccountId(accountId.toString())
                    .setAmount(Money.newBuilder()
                            .setAmount(amount.longValue())
                            .setCurrency(currency)
                            .build())
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            ReserveBalanceResponse response = accountServiceStub.reserveBalance(request);
            
            String reservationId = response.getReservationId();
            log.info("gRPC Client: ReserveBalance success - reservationId={}", reservationId);
            return reservationId;

        } catch (StatusRuntimeException e) {
            handleGrpcError("ReserveBalance", e);
            throw e;
        }
    }

    /**
     * Commit reservation - Step 3 of payment flow (after transaction is recorded).
     * Permanently deducts money from the account.
     * 
     * @param reservationId The reservation to commit
     * @param transactionId The transaction ID from transaction-service
     * @throws ReservationException if reservation not found or invalid state
     */
    public void commitReservation(String reservationId, String transactionId) {
        log.info("gRPC Client: CommitReservation reservationId={}, transactionId={}", 
                reservationId, transactionId);

        try {
            CommitReservationRequest request = CommitReservationRequest.newBuilder()
                    .setReservationId(reservationId)
                    .setTransactionId(transactionId)
                    .build();

            accountServiceStub.commitReservation(request);
            
            log.info("gRPC Client: CommitReservation success");

        } catch (StatusRuntimeException e) {
            handleGrpcError("CommitReservation", e);
            throw e;
        }
    }

    /**
     * Release reservation - Rollback path.
     * Unlocks money without deducting it.
     * 
     * @param reservationId The reservation to release
     * @param reason Why the reservation is being released (for audit)
     * @throws ReservationException if reservation not found or invalid state
     */
    public void releaseReservation(String reservationId, String reason) {
        log.info("gRPC Client: ReleaseReservation reservationId={}, reason={}", 
                reservationId, reason);

        try {
            ReleaseReservationRequest request = ReleaseReservationRequest.newBuilder()
                    .setReservationId(reservationId)
                    .setReason(reason)
                    .build();

            accountServiceStub.releaseReservation(request);
            
            log.info("gRPC Client: ReleaseReservation success");

        } catch (StatusRuntimeException e) {
            handleGrpcError("ReleaseReservation", e);
            throw e;
        }
    }

    /**
     * Translates gRPC status codes to domain exceptions.
     * This keeps gRPC concerns in the client layer,
     * and business logic uses familiar exceptions.
     */
    private void handleGrpcError(String operation, StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();
        String description = e.getStatus().getDescription();

        log.error("gRPC Client: {} failed - code={}, description={}", operation, code, description);

        switch (code) {
            case NOT_FOUND -> throw new AccountNotFoundException(
                    description != null ? description : "Resource not found"
            );
            case FAILED_PRECONDITION -> {
                // Could be insufficient balance or invalid reservation state
                if (description != null && description.contains("balance")) {
                    throw new InsufficientBalanceException(description);
                }
                throw new ReservationException(description != null ? description : "Precondition failed");
            }
            case INVALID_ARGUMENT -> throw new IllegalArgumentException(
                    description != null ? description : "Invalid argument"
            );
            case UNAVAILABLE -> throw new RuntimeException(
                    "Account service is unavailable. Please try again later."
            );
            case DEADLINE_EXCEEDED -> throw new RuntimeException(
                    "Account service request timed out. Please try again."
            );
            default -> throw new RuntimeException(
                    "Account service error: " + code + " - " + description
            );
        }
    }
}
