package com.bankingmanagement.transactionservice.repository;

import com.bankingmanagement.transactionservice.model.Transaction;
import com.bankingmanagement.transactionservice.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    List<Transaction> findBySourceAccountId(UUID accountId);
    
    List<Transaction> findByDestinationAccountId(UUID accountId);
    
    List<Transaction> findBySourceAccountIdOrDestinationAccountId(UUID sourceId, UUID destId);
    
    List<Transaction> findByStatus(TransactionStatus status);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    
    Optional<Transaction> findByPaymentId(UUID paymentId);
}
