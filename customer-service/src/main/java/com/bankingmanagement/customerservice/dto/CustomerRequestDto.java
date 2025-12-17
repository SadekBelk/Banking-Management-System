package com.bankingmanagement.customerservice.dto;

import com.bankingmanagement.customerservice.model.KycStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerRequestDto {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    private String email;

    private String phoneNumber;

    @Past
    private LocalDate dateOfBirth;

    private KycStatus kycStatus;
}