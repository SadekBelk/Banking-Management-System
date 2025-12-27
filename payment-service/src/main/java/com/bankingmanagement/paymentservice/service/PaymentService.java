package com.bankingmanagement.paymentservice.service;

import com.bankingmanagement.paymentservice.dto.PaymentRequestDto;
import com.bankingmanagement.paymentservice.dto.PaymentResponseDto;
import com.bankingmanagement.paymentservice.model.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponseDto createPayment(PaymentRequestDto request);

    PaymentResponseDto getPaymentById(UUID paymentId);

    PaymentResponseDto getPaymentByReferenceNumber(String referenceNumber);

    List<PaymentResponseDto> getPaymentsByAccountId(UUID accountId);

    List<PaymentResponseDto> getPaymentsByStatus(PaymentStatus status);

    PaymentResponseDto cancelPayment(UUID paymentId);

    PaymentResponseDto processPayment(UUID paymentId);

}
