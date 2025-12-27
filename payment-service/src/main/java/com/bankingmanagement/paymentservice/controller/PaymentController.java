package com.bankingmanagement.paymentservice.controller;

import com.bankingmanagement.paymentservice.dto.PaymentRequestDto;
import com.bankingmanagement.paymentservice.dto.PaymentResponseDto;
import com.bankingmanagement.paymentservice.model.PaymentStatus;
import com.bankingmanagement.paymentservice.service.PaymentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "API for managing banking PAYMENTS")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ✅ Create payment
    @PostMapping
    @Operation(summary = "Create Payment", description = "Creates and processes a new payment")
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto request
    ) {
        PaymentResponseDto response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ Get payment by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get Payment by ID", description = "Retrieves a payment by its unique ID")
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    // ✅ Get payment by reference number
    @GetMapping("/reference/{referenceNumber}")
    @Operation(summary = "Get Payment by Reference Number", description = "Retrieves a payment by its reference number")
    public ResponseEntity<PaymentResponseDto> getPaymentByReference(
            @PathVariable String referenceNumber
    ) {
        return ResponseEntity.ok(paymentService.getPaymentByReferenceNumber(referenceNumber));
    }

    // ✅ Get all payments for an account (both sent and received)
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all payments for an account", description = "Retrieves all payments for a specific account, including both sent and received payments")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByAccount(
            @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsByAccountId(accountId));
    }

    // ✅ Get payments by status
    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status", description = "Retrieves all payments with a specific status")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByStatus(
            @PathVariable PaymentStatus status
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    // ✅ Cancel payment
    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel payment", description = "Cancels an existing payment")
    public ResponseEntity<PaymentResponseDto> cancelPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.cancelPayment(id));
    }

    // ✅ Process payment (trigger payment execution)
    @PostMapping("/{id}/process")
    @Operation(summary = "Process payment", description = "Processes an existing payment")
    public ResponseEntity<PaymentResponseDto> processPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.processPayment(id));
    }

}

