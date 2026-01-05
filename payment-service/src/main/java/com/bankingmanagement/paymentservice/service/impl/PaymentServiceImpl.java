package com.bankingmanagement.paymentservice.service.impl;

import com.banking.proto.transaction.TransactionType;
import com.bankingmanagement.paymentservice.client.AccountClient;
import com.bankingmanagement.paymentservice.dto.PaymentRequestDto;
import com.bankingmanagement.paymentservice.dto.PaymentResponseDto;
import com.bankingmanagement.paymentservice.event.PaymentEventPublisher;
import com.bankingmanagement.paymentservice.exception.AccountNotFoundException;
import com.bankingmanagement.paymentservice.exception.InsufficientBalanceException;
import com.bankingmanagement.paymentservice.exception.InvalidPaymentException;
import com.bankingmanagement.paymentservice.exception.PaymentNotFoundException;
import com.bankingmanagement.paymentservice.exception.ReservationException;
import com.bankingmanagement.paymentservice.exception.TransactionException;
import com.bankingmanagement.paymentservice.grpc.AccountGrpcClient;
import com.bankingmanagement.paymentservice.grpc.TransactionGrpcClient;
import com.bankingmanagement.paymentservice.mapper.PaymentMapper;
import com.bankingmanagement.paymentservice.model.Payment;
import com.bankingmanagement.paymentservice.model.PaymentStatus;
import com.bankingmanagement.paymentservice.model.PaymentType;
import com.bankingmanagement.paymentservice.repository.PaymentRepository;
import com.bankingmanagement.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payment Service Implementation - THE ORCHESTRATOR
 * 
 * This service orchestrates the entire payment flow using gRPC:
 * 
 * PAYMENT FLOW (Full Transfer):
 * 1️⃣ ReserveBalance (gRPC → account-service) - Lock money from source
 * 2️⃣ CreateTransaction (gRPC → transaction-service) - Record in ledger as PENDING
 * 3️⃣ CreditBalance (gRPC → account-service) - Add money to destination
 * 4️⃣ CommitReservation (gRPC → account-service) - Permanently deduct from source
 * 5️⃣ CompleteTransaction (gRPC → transaction-service) - Mark ledger entry COMPLETED
 * 
 * ROLLBACK FLOW (if any step fails):
 * - ReleaseReservation (gRPC → account-service) - Unlock the money
 * - FailTransaction (gRPC → transaction-service) - Mark ledger entry FAILED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final AccountClient accountClient;                // Legacy HTTP client (for account existence check)
    private final AccountGrpcClient accountGrpcClient;        // gRPC client (for balance operations)
    private final TransactionGrpcClient transactionGrpcClient; // gRPC client (for ledger operations)
    private final PaymentEventPublisher eventPublisher;       // Kafka event publisher

    @Value("${account-service.base-url}")
    private String accountServiceBaseUrl;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto request) {

        // 0️⃣ Validate: Source and destination accounts cannot be the same
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new InvalidPaymentException("Source and destination accounts cannot be the same");
        }

        // 1️⃣ Validate source account exists
        boolean sourceExists = accountClient.accountExists(
                request.getSourceAccountId(),
                accountServiceBaseUrl
        );
        if (!sourceExists) {
            throw new AccountNotFoundException("Source account not found: " + request.getSourceAccountId());
        }

        // 2️⃣ Validate destination account exists
        boolean destExists = accountClient.accountExists(
                request.getDestinationAccountId(),
                accountServiceBaseUrl
        );
        if (!destExists) {
            throw new AccountNotFoundException("Destination account not found: " + request.getDestinationAccountId());
        }

        // 3️⃣ Map request to entity
        Payment payment = paymentMapper.toEntity(request);

        // 4️⃣ Apply business rules
        payment.setReferenceNumber(generateReferenceNumber());
        payment.setIdempotencyKey(generateIdempotencyKey());  // For gRPC reservation
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());

        if (payment.getCurrency() == null) {
            payment.setCurrency("USD");
        }

        // 5️⃣ Persist
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment created: id={}, reference={}, idempotencyKey={}", 
                savedPayment.getId(), savedPayment.getReferenceNumber(), savedPayment.getIdempotencyKey());

        // 6️⃣ Publish Kafka event - PAYMENT_INITIATED
        eventPublisher.publishPaymentInitiated(savedPayment);

        // 7️⃣ Return response DTO
        return paymentMapper.toResponseDto(savedPayment);
    }

    @Override
    public PaymentResponseDto getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        return paymentMapper.toResponseDto(payment);
    }

    @Override
    public PaymentResponseDto getPaymentByReferenceNumber(String referenceNumber) {
        Payment payment = paymentRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with reference: " + referenceNumber
                ));

        return paymentMapper.toResponseDto(payment);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByAccountId(UUID accountId) {
        return paymentRepository
                .findBySourceAccountIdOrDestinationAccountId(accountId, accountId)
                .stream()
                .map(paymentMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status)
                .stream()
                .map(paymentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponseDto cancelPayment(UUID paymentId) {
        log.info("Cancelling payment: paymentId={}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Can only cancel PENDING or PROCESSING payments
        if (payment.getStatus() != PaymentStatus.PENDING && 
            payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentException(
                    "Cannot cancel payment with status: " + payment.getStatus()
            );
        }

        // If there's a reservation, release it
        if (payment.getReservationId() != null) {
            try {
                log.info("Releasing reservation {} for cancelled payment", payment.getReservationId());
                accountGrpcClient.releaseReservation(
                        payment.getReservationId(), 
                        "Payment cancelled by user"
                );
                log.info("Reservation released successfully");
            } catch (Exception e) {
                log.error("Failed to release reservation during cancel: {}", e.getMessage());
                // Continue with cancellation even if release fails
            }
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setUpdatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment cancelled: paymentId={}, reference={}", 
                payment.getId(), payment.getReferenceNumber());

        // Publish Kafka event - PAYMENT_CANCELLED
        eventPublisher.publishPaymentCancelled(savedPayment);
        
        return paymentMapper.toResponseDto(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayment(UUID paymentId) {
        log.info("Processing payment: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Can only process PENDING payments
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentException(
                    "Cannot process payment with status: " + payment.getStatus()
            );
        }

        // Mark as processing
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        // Publish Kafka event - PAYMENT_PROCESSING
        eventPublisher.publishPaymentProcessing(payment);

        String reservationId = null;
        String transactionId = null;

        try {
            // ═══════════════════════════════════════════════════════════════════
            // STEP 1️⃣: RESERVE BALANCE (gRPC → account-service)
            // ═══════════════════════════════════════════════════════════════════
            log.info("Step 1: Reserving balance from source account");
            
            reservationId = accountGrpcClient.reserveBalance(
                    payment.getSourceAccountId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getIdempotencyKey()
            );
            
            payment.setReservationId(reservationId);
            paymentRepository.save(payment);
            log.info("Step 1 complete: reservationId={}", reservationId);

            // ═══════════════════════════════════════════════════════════════════
            // STEP 2️⃣: CREATE TRANSACTION RECORD (gRPC → transaction-service)
            // ═══════════════════════════════════════════════════════════════════
            log.info("Step 2: Creating transaction record in ledger");
            
            TransactionGrpcClient.CreateTransactionResult txnResult = transactionGrpcClient.createTransaction(
                    payment.getSourceAccountId(),
                    payment.getDestinationAccountId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    TransactionType.TRANSFER,
                    payment.getId(),                 // paymentId (UUID)
                    reservationId,                   // reservationId (String)
                    payment.getDescription(),        // description (String)
                    payment.getIdempotencyKey()      // idempotencyKey (String)
            );
            
            transactionId = txnResult.transactionId();
            payment.setTransactionId(transactionId);
            paymentRepository.save(payment);
            log.info("Step 2 complete: transactionId={}, referenceNumber={}", 
                    transactionId, txnResult.referenceNumber());

            // ═══════════════════════════════════════════════════════════════════
            // STEP 3️⃣: CREDIT DESTINATION ACCOUNT (gRPC → account-service)
            // ═══════════════════════════════════════════════════════════════════
            log.info("Step 3: Crediting destination account");
            
            BigDecimal newDestBalance = accountGrpcClient.creditBalance(
                    payment.getDestinationAccountId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    transactionId,  // Use transaction ID for audit trail
                    payment.getDescription()
            );
            
            log.info("Step 3 complete: destination credited, newBalance={}", newDestBalance);

            // ═══════════════════════════════════════════════════════════════════
            // STEP 4️⃣: COMMIT RESERVATION (gRPC → account-service)
            // ═══════════════════════════════════════════════════════════════════
            log.info("Step 4: Committing reservation (permanently deducting from source)");
            
            accountGrpcClient.commitReservation(reservationId, transactionId);
            log.info("Step 4 complete: reservation committed");

            // ═══════════════════════════════════════════════════════════════════
            // STEP 5️⃣: COMPLETE TRANSACTION (gRPC → transaction-service)
            // ═══════════════════════════════════════════════════════════════════
            log.info("Step 5: Completing transaction record");
            
            transactionGrpcClient.completeTransaction(transactionId);
            log.info("Step 5 complete: transaction marked as COMPLETED");

            // ═══════════════════════════════════════════════════════════════════
            // SUCCESS! Payment completed
            // ═══════════════════════════════════════════════════════════════════
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(Instant.now());
            payment.setUpdatedAt(Instant.now());

            // Publish Kafka event - PAYMENT_COMPLETED
            eventPublisher.publishPaymentCompleted(payment);
            
            log.info("Payment processing SUCCESS: paymentId={}, transactionId={}, reference={}", 
                    payment.getId(), transactionId, payment.getReferenceNumber());

        } catch (InsufficientBalanceException e) {
            // Insufficient balance - no reservation was made, just fail
            log.warn("Payment failed - insufficient balance: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient balance: " + e.getMessage());
            payment.setUpdatedAt(Instant.now());

            // Publish Kafka event - PAYMENT_FAILED
            eventPublisher.publishPaymentFailed(payment, e.getMessage());

        } catch (AccountNotFoundException e) {
            // Account not found - no reservation was made, just fail
            log.warn("Payment failed - account not found: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Account not found: " + e.getMessage());
            payment.setUpdatedAt(Instant.now());

            // Publish Kafka event - PAYMENT_FAILED
            eventPublisher.publishPaymentFailed(payment, e.getMessage());

        } catch (TransactionException e) {
            // Transaction service error - rollback reservation and fail transaction
            log.error("Payment failed - transaction error: {}", e.getMessage());
            handleRollback(payment, reservationId, transactionId, "Transaction error: " + e.getMessage());

        } catch (ReservationException e) {
            // Reservation issue - need to check if we need to release
            log.error("Payment failed - reservation error: {}", e.getMessage());
            handleRollback(payment, reservationId, transactionId, "Reservation error: " + e.getMessage());

        } catch (Exception e) {
            // Unexpected error - try to rollback
            log.error("Payment failed - unexpected error: {}", e.getMessage(), e);
            handleRollback(payment, reservationId, transactionId, "Unexpected error: " + e.getMessage());
        }

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseDto(savedPayment);
    }

    /**
     * Handle rollback when payment fails after reservation was made.
     * Releases the reserved balance back to available and marks transaction as failed.
     */
    private void handleRollback(Payment payment, String reservationId, String transactionId, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        payment.setUpdatedAt(Instant.now());

        // If transaction was created, mark it as failed
        if (transactionId != null) {
            try {
                log.info("Rolling back: marking transaction {} as FAILED", transactionId);
                transactionGrpcClient.failTransaction(transactionId, reason);
                log.info("Rollback: transaction marked as FAILED");
            } catch (Exception txnError) {
                // Log but don't throw - payment is already failed
                log.error("Failed to mark transaction as failed during rollback: {}", 
                        txnError.getMessage(), txnError);
            }
        }

        // If reservation was made, release it
        if (reservationId != null) {
            try {
                log.info("Rolling back: releasing reservation {}", reservationId);
                accountGrpcClient.releaseReservation(reservationId, reason);
                log.info("Rollback complete: reservation released");
            } catch (Exception rollbackError) {
                // Log but don't throw - payment is already failed
                log.error("Failed to release reservation during rollback: {}", 
                        rollbackError.getMessage(), rollbackError);
                payment.setFailureReason(reason + " (WARNING: Failed to release reservation)");
            }
        }

        // Publish Kafka event - PAYMENT_FAILED
        eventPublisher.publishPaymentFailed(payment, reason);
    }

    // 🔐 Internal helpers
    private String generateReferenceNumber() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateIdempotencyKey() {
        return "IDEM-" + UUID.randomUUID().toString();
    }

}

