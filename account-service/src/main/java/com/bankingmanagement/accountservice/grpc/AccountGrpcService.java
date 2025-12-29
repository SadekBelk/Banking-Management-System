package com.bankingmanagement.accountservice.grpc;

import com.bankingmanagement.accountservice.exception.AccountNotFoundException;
import com.bankingmanagement.accountservice.exception.InsufficientBalanceException;
import com.bankingmanagement.accountservice.exception.InvalidReservationStateException;
import com.bankingmanagement.accountservice.exception.ReservationNotFoundException;
import com.bankingmanagement.accountservice.model.Account;
import com.bankingmanagement.accountservice.model.BalanceReservation;
import com.bankingmanagement.accountservice.repository.AccountRepository;
import com.bankingmanagement.accountservice.service.BalanceReservationService;
import com.bankingmanagement.accountservice.service.AccountService;
import com.banking.proto.account.*;
import com.banking.proto.common.Money;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * gRPC Server implementation for AccountService.
 * 
 * This class is the gRPC entry point - it receives gRPC calls from payment-service
 * and delegates to the domain services.
 * 
 * KEY ARCHITECTURAL RULES:
 * - account-service is the gRPC SERVER (it owns the data)
 * - payment-service is the gRPC CLIENT (it orchestrates)
 * - This service NEVER calls other services
 * - Errors are communicated via gRPC status codes, NOT embedded in responses
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final BalanceReservationService reservationService;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    /**
     * Get available balance for an account.
     * Available = Actual Balance - Pending Reservations
     */
    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
        log.info("gRPC GetBalance: accountId={}", request.getAccountId());

        try {
            UUID accountId = parseUUID(request.getAccountId(), "account_id");
            
            // Get actual account for currency info
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

            // Get available balance (actual - reserved)
            BigDecimal availableBalance = reservationService.getAvailableBalance(accountId);

            GetBalanceResponse response = GetBalanceResponse.newBuilder()
                    .setAvailableBalance(Money.newBuilder()
                            .setAmount(availableBalance.longValue()) // Convert to cents/minor units in real system
                            .setCurrency(account.getCurrency())
                            .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC GetBalance completed: accountId={}, availableBalance={}", 
                    accountId, availableBalance);

        } catch (AccountNotFoundException e) {
            log.warn("gRPC GetBalance failed: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            log.warn("gRPC GetBalance failed: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC GetBalance unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    /**
     * Reserve balance - Step 1 of payment flow.
     * Locks money without deducting it.
     * Idempotent: same idempotency_key returns same reservation.
     */
    @Override
    public void reserveBalance(ReserveBalanceRequest request, StreamObserver<ReserveBalanceResponse> responseObserver) {
        log.info("gRPC ReserveBalance: accountId={}, amount={}, idempotencyKey={}", 
                request.getAccountId(), request.getAmount(), request.getIdempotencyKey());

        try {
            // 1️⃣ Validate request
            UUID accountId = parseUUID(request.getAccountId(), "account_id");
            validateMoney(request.getAmount());
            validateIdempotencyKey(request.getIdempotencyKey());

            // 2️⃣ Convert protobuf Money to BigDecimal
            BigDecimal amount = BigDecimal.valueOf(request.getAmount().getAmount());
            String currency = request.getAmount().getCurrency();

            // 3️⃣ Call domain service
            BalanceReservation reservation = reservationService.reserveBalance(
                    accountId, amount, currency, request.getIdempotencyKey()
            );

            // 4️⃣ Build response
            ReserveBalanceResponse response = ReserveBalanceResponse.newBuilder()
                    .setReservationId(reservation.getId().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC ReserveBalance completed: reservationId={}", reservation.getId());

        } catch (AccountNotFoundException e) {
            log.warn("gRPC ReserveBalance failed - account not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (InsufficientBalanceException e) {
            log.warn("gRPC ReserveBalance failed - insufficient balance: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (IllegalArgumentException e) {
            log.warn("gRPC ReserveBalance failed - invalid argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC ReserveBalance unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    /**
     * Commit reservation - Step 3 of payment flow.
     * Permanently deducts money from the account.
     * Called AFTER transaction ledger entry exists.
     */
    @Override
    public void commitReservation(CommitReservationRequest request, StreamObserver<CommitReservationResponse> responseObserver) {
        log.info("gRPC CommitReservation: reservationId={}, transactionId={}", 
                request.getReservationId(), request.getTransactionId());

        try {
            // 1️⃣ Validate request
            UUID reservationId = parseUUID(request.getReservationId(), "reservation_id");
            validateTransactionId(request.getTransactionId());

            // 2️⃣ Call domain service
            reservationService.commitReservation(reservationId, request.getTransactionId());

            // 3️⃣ Return empty response (success indicated by no error)
            responseObserver.onNext(CommitReservationResponse.getDefaultInstance());
            responseObserver.onCompleted();

            log.info("gRPC CommitReservation completed: reservationId={}", reservationId);

        } catch (ReservationNotFoundException e) {
            log.warn("gRPC CommitReservation failed - reservation not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (InvalidReservationStateException e) {
            log.warn("gRPC CommitReservation failed - invalid state: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (IllegalArgumentException e) {
            log.warn("gRPC CommitReservation failed - invalid argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC CommitReservation unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    /**
     * Release reservation - Rollback path.
     * Unlocks money without deducting it.
     * Called when payment fails or times out.
     */
    @Override
    public void releaseReservation(ReleaseReservationRequest request, StreamObserver<ReleaseReservationResponse> responseObserver) {
        log.info("gRPC ReleaseReservation: reservationId={}, reason={}", 
                request.getReservationId(), request.getReason());

        try {
            // 1️⃣ Validate request
            UUID reservationId = parseUUID(request.getReservationId(), "reservation_id");

            // 2️⃣ Call domain service
            reservationService.releaseReservation(reservationId, request.getReason());

            // 3️⃣ Return empty response
            responseObserver.onNext(ReleaseReservationResponse.getDefaultInstance());
            responseObserver.onCompleted();

            log.info("gRPC ReleaseReservation completed: reservationId={}", reservationId);

        } catch (ReservationNotFoundException e) {
            log.warn("gRPC ReleaseReservation failed - reservation not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (InvalidReservationStateException e) {
            log.warn("gRPC ReleaseReservation failed - invalid state: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (IllegalArgumentException e) {
            log.warn("gRPC ReleaseReservation failed - invalid argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC ReleaseReservation unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    /**
     * Credit balance - Add funds to destination account.
     * This completes the transfer by adding money to the recipient.
     */
    @Override
    public void creditBalance(CreditBalanceRequest request, StreamObserver<CreditBalanceResponse> responseObserver) {
        log.info("gRPC CreditBalance: accountId={}, amount={} {}, referenceId={}", 
                request.getAccountId(), request.getAmount().getAmount(), 
                request.getAmount().getCurrency(), request.getReferenceId());

        try {
            // 1️⃣ Validate request
            UUID accountId = parseUUID(request.getAccountId(), "account_id");
            validateMoney(request.getAmount());
            if (request.getReferenceId() == null || request.getReferenceId().isBlank()) {
                throw new IllegalArgumentException("reference_id is required");
            }

            // 2️⃣ Get account
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

            // 3️⃣ Credit the account (add funds)
            BigDecimal creditAmount = BigDecimal.valueOf(request.getAmount().getAmount());
            BigDecimal newBalance = account.getBalance().add(creditAmount);
            account.setBalance(newBalance);
            accountRepository.save(account);

            log.info("gRPC CreditBalance: Account {} credited with {} {}. New balance: {}", 
                    accountId, creditAmount, request.getAmount().getCurrency(), newBalance);

            // 4️⃣ Build response with new balance
            CreditBalanceResponse response = CreditBalanceResponse.newBuilder()
                    .setNewBalance(Money.newBuilder()
                            .setAmount(newBalance.longValue())
                            .setCurrency(account.getCurrency())
                            .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC CreditBalance completed: accountId={}, newBalance={}", accountId, newBalance);

        } catch (AccountNotFoundException e) {
            log.warn("gRPC CreditBalance failed - account not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (IllegalArgumentException e) {
            log.warn("gRPC CreditBalance failed - invalid argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("gRPC CreditBalance unexpected error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error")
                    .asRuntimeException());
        }
    }

    // ================== Validation Helpers ==================

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

    private void validateMoney(Money money) {
        if (money == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (money.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (money.getCurrency() == null || money.getCurrency().isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("idempotency_key is required");
        }
    }

    private void validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transaction_id is required");
        }
    }
}
