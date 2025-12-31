package com.bankingmanagement.paymentservice.mapper;

import com.bankingmanagement.paymentservice.dto.PaymentRequestDto;
import com.bankingmanagement.paymentservice.dto.PaymentResponseDto;
import com.bankingmanagement.paymentservice.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    /* =========================
       CREATE
       ========================= */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "reservationId", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    Payment toEntity(PaymentRequestDto dto);

    /* =========================
       RESPONSE
       ========================= */

    PaymentResponseDto toResponseDto(Payment payment);

    /* =========================
       UPDATE (future-proof)
       ========================= */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "sourceAccountId", ignore = true)
    @Mapping(target = "destinationAccountId", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "reservationId", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    void updateEntityFromDto(
            PaymentRequestDto dto,
            @MappingTarget Payment payment
    );

}
