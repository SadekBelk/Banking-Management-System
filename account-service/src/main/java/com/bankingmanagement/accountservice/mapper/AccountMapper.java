package com.bankingmanagement.accountservice.mapper;

import com.bankingmanagement.accountservice.dto.AccountRequestDto;
import com.bankingmanagement.accountservice.dto.AccountResponseDto;
import com.bankingmanagement.accountservice.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    /* =========================
       CREATE
       ========================= */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(source = "initialBalance", target = "balance")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toEntity(AccountRequestDto dto);

    /* =========================
       RESPONSE
       ========================= */

    AccountResponseDto toResponseDto(Account account);

    /* =========================
       UPDATE (future-proof)
       ========================= */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(
            AccountRequestDto dto,
            @MappingTarget Account account
    );

}
