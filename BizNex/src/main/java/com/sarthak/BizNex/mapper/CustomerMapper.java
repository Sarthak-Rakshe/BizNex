package com.sarthak.BizNex.mapper;

import com.sarthak.BizNex.dto.CustomerDto;
import com.sarthak.BizNex.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);
    @Mapping(target = "customerRegistrationDate", source = "customerRegistrationDate", qualifiedByName = "formatDate")
    CustomerDto toDto(Customer customer);

    @Named("formatDate")
    static String formatDate(LocalDateTime date) {
        if (date == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        return date.format(formatter);
    }

//    CustomerDto toDto(Customer customer);

    @Mapping(target = "customerRegistrationDate", ignore = true)
    Customer toEntity(CustomerDto customerDto);

    List<CustomerDto> toDtoList(List<Customer> customers);

    List<Customer> toEntityList(List<CustomerDto> customerDtos);
}

