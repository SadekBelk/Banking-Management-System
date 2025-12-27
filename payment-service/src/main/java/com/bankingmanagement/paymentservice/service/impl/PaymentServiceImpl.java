package com.bankingmanagement.paymentservice.service.impl;

import com.bankingmanagement.paymentservice.client.AccountClient;
import com.bankingmanagement.paymentservice.dto.PaymentRequestDto;
import com.bankingmanagement.paymentservice.dto.PaymentResponseDto;
import com.bankingmanagement.paymentservice.exception.AccountNotFoundException;
import com.bankingmanagement.paymentservice.exception.InvalidPaymentException;
import com.bankingmanagement.paymentservice.exception.PaymentNotFoundException;
import com.bankingmanagement.paymentservice.mapper.PaymentMapper;
import com.bankingmanagement.paymentservice.model.Payment;
import com.bankingmanagement.paymentservice.model.PaymentStatus;
import com.bankingmanagement.paymentservice.repository.PaymentRepository;
import com.bankingmanagement.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final AccountClient accountClient;

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
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());

        if (payment.getCurrency() == null) {
            payment.setCurrency("USD");
        }

        // 5️⃣ Persist
        Payment savedPayment = paymentRepository.save(payment);

        // 6️⃣ Return response DTO
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
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Can only cancel PENDING payments
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentException(
                    "Cannot cancel payment with status: " + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setUpdatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseDto(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayment(UUID paymentId) {
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

        try {
            // TODO: Integrate with account-service to debit/credit accounts
            // This would involve:
            // 1. Debit source account
            // 2. Credit destination account
            // For now, we simulate success

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(Instant.now());
            payment.setUpdatedAt(Instant.now());

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            payment.setUpdatedAt(Instant.now());
        }

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseDto(savedPayment);
    }

    // 🔐 Internal helper (not exposed)
    private String generateReferenceNumber() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

}

