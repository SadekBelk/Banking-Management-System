package com.bankingmanagement.accountservice.repository;

import com.bankingmanagement.accountservice.model.Account;
import com.bankingmanagement.accountservice.model.AccountStatus;
import com.bankingmanagement.accountservice.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    // 🔐 uniqueness checks
    boolean existsByAccountNumber(String accountNumber);

    // 🔍 lookup methods
    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(UUID customerId);

    List<Account> findByCustomerIdAndStatus(UUID customerId, AccountStatus status);

    List<Account> findByCustomerIdAndType(UUID customerId, AccountType type);
}
