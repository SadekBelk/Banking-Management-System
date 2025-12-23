package com.bankingmanagement.transactionservice.repository;

import com.bankingmanagement.transactionservice.model.Transaction;
import com.bankingmanagement.transactionservice.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountId(Long accountId);
    List<Transaction> findByToAccountId(Long accountId);
    List<Transaction> findByStatus(TransactionStatus status);
}
