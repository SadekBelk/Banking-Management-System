package com.bankingmanagement.customerservice.dto;

import com.bankingmanagement.customerservice.model.KycStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CustomerResponseDto {

    private UUID id;
    private UUID externalUserId;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    private LocalDate dateOfBirth;
    private KycStatus kycStatus;

    private Instant createdAt;
    private Instant updatedAt;
}
