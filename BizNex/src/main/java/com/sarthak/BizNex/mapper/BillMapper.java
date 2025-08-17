package com.sarthak.BizNex.mapper;

import com.sarthak.BizNex.dto.BillDto;
import com.sarthak.BizNex.dto.BillItemDto;
import com.sarthak.BizNex.entity.Bill;
import com.sarthak.BizNex.entity.BillItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BillMapper {
    BillMapper INSTANCE = Mappers.getMapper(BillMapper.class);

    @Mapping(target = "customer", source = "customer")
    @Mapping(target = "billDate", source = "billDate", qualifiedByName = "formatDate")
    BillDto toDto(Bill bill);

    @Named("formatDate")
    static String formatDate(LocalDateTime date) {
        if (date == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        return date.format(formatter);
    }

    @Mapping(target = "billDate", ignore = true)
    Bill toEntity(BillDto billDto);

    List<BillDto> toDtoList(List<Bill> bills);

    List<BillItem> toEntityList(List<BillItemDto> billItems);
}