package com.bankingmanagement.accountservice.service;

import com.bankingmanagement.accountservice.dto.AccountRequestDto;
import com.bankingmanagement.accountservice.dto.AccountResponseDto;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponseDto createAccount(AccountRequestDto request);

    AccountResponseDto getAccountById(UUID accountId);

    List<AccountResponseDto> getAccountsByCustomerId(UUID customerId);

    void deactivateAccount(UUID accountId);

}
