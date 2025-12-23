package com.bankingmanagement.transactionservice.mapper;

import com.bankingmanagement.transactionservice.dto.TransactionRequestDto;
import com.bankingmanagement.transactionservice.dto.TransactionResponseDto;
import com.bankingmanagement.transactionservice.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    /**
     * Convert Entity to Response DTO (for API responses)
     */
    public TransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionResponseDto dto = new TransactionResponseDto();
        dto.setId(transaction.getId());
        dto.setFromAccountId(transaction.getFromAccountId());
        dto.setToAccountId(transaction.getToAccountId());
        dto.setAmount(transaction.getAmount());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setStatus(transaction.getStatus());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        dto.setDescription(transaction.getDescription());
        return dto;
    }

    /**
     * Convert Request DTO to Entity (for creating new transactions)
     */
    public Transaction toEntity(TransactionRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setFromAccountId(requestDto.getFromAccountId());
        transaction.setToAccountId(requestDto.getToAccountId());
        transaction.setAmount(requestDto.getAmount());
        transaction.setTransactionType(requestDto.getTransactionType());
        transaction.setDescription(requestDto.getDescription());
        // Status is set by service layer, not from request
        return transaction;
    }
}
