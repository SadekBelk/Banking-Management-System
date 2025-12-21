package com.bankingmanagement.accountservice.service.impl;

import com.bankingmanagement.accountservice.dto.AccountRequestDto;
import com.bankingmanagement.accountservice.dto.AccountResponseDto;
import com.bankingmanagement.accountservice.exception.AccountNotFoundException;
import com.bankingmanagement.accountservice.exception.InvalidInitialBalanceException;
import com.bankingmanagement.accountservice.mapper.AccountMapper;
import com.bankingmanagement.accountservice.model.Account;
import com.bankingmanagement.accountservice.model.AccountStatus;
import com.bankingmanagement.accountservice.repository.AccountRepository;
import com.bankingmanagement.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    public AccountResponseDto createAccount(AccountRequestDto request) {

        // 1️⃣ Validate initial balance
        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidInitialBalanceException("Initial balance cannot be negative");
        }

        // 2️⃣ Map request to entity (controlled mapping)
        Account account = accountMapper.toEntity(request);

        // 3️⃣ Apply business rules
        account.setAccountNumber(generateAccountNumber());
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());

        if (account.getCurrency() == null) {
            account.setCurrency("USD");
        }

        // 4️⃣ Persist
        Account savedAccount = accountRepository.save(account);

        // 5️⃣ Return response DTO
        return accountMapper.toResponseDto(savedAccount);
    }

    @Override
    public AccountResponseDto getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with id: " + accountId
                ));

        return accountMapper.toResponseDto(account);
    }

    @Override
    public List<AccountResponseDto> getAccountsByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(accountMapper::toResponseDto)
                .toList();
    }

    @Override
    public void deactivateAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with id: " + accountId
                ));

        account.setStatus(AccountStatus.INACTIVE);
        account.setUpdatedAt(Instant.now());

        accountRepository.save(account);
    }

    // 🔐 Internal helper (not exposed)
    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID();
    }

}
