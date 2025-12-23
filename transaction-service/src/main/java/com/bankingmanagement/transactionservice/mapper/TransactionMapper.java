package com.bankingmanagement.transactionservice.mapper;

import com.bankingmanagement.transactionservice.dto.TransactionDTO;
import com.bankingmanagement.transactionservice.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    
    public TransactionDTO toDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        
        return TransactionDTO.builder()
                .id(transaction.getId())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .description(transaction.getDescription())
                .build();
    }
    
    public Transaction toEntity(TransactionDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return Transaction.builder()
                .id(dto.getId())
                .fromAccountId(dto.getFromAccountId())
                .toAccountId(dto.getToAccountId())
                .amount(dto.getAmount())
                .transactionType(dto.getTransactionType())
                .status(dto.getStatus())
                .description(dto.getDescription())
                .build();
    }
}
