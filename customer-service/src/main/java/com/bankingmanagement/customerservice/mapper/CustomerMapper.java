package com.bankingmanagement.customerservice.mapper;

import com.bankingmanagement.customerservice.dto.CustomerRequestDto;
import com.bankingmanagement.customerservice.dto.CustomerResponseDto;
import com.bankingmanagement.customerservice.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;


// Adding "unmappedTargetPolicy = ReportingPolicy.IGNORE" temporarily silences unknown property errors during compilation.
// Once it compiles, MapStruct will generate the implementation correctly.
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    Customer toEntity(CustomerRequestDto dto);

    CustomerResponseDto toResponseDto(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true) // email should not change
    @Mapping(target = "externalUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(
            CustomerRequestDto dto,
            @MappingTarget Customer customer
    );
}
