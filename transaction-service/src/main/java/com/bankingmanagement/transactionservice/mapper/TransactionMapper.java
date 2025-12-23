package com.bankingmanagement.transactionservice.mapper;

import com.bankingmanagement.transactionservice.dto.TransactionRequestDto;
import com.bankingmanagement.transactionservice.dto.TransactionResponseDto;
import com.bankingmanagement.transactionservice.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    /* =========================
       CREATE
       ========================= */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(TransactionRequestDto requestDto);

    /* =========================
       RESPONSE
       ========================= */

    TransactionResponseDto toResponseDto(Transaction transaction);

    /* =========================
       UPDATE (future-proof)
       ========================= */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fromAccountId", ignore = true)
    @Mapping(target = "toAccountId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(
            TransactionRequestDto requestDto,
            @MappingTarget Transaction transaction
    );
}
