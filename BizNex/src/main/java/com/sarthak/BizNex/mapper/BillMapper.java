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

@Mapper(componentModel = "spring", uses = {CustomerMapper.class, BillItemMapper.class})
public interface BillMapper {
    BillMapper INSTANCE = Mappers.getMapper(BillMapper.class);

    @Mapping(target = "customer", source = "customer")
    @Mapping(target = "billDate", source = "billDate", qualifiedByName = "formatBillDate")
    BillDto toDto(Bill bill);

    @Named("formatBillDate")
    static String formatBillDate(LocalDateTime date) {
        if (date == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        return date.format(formatter);
    }

    // Ignore generated id and server-managed date
    @Mapping(target = "billId", ignore = true)
    @Mapping(target = "billDate", ignore = true)
    Bill toEntity(BillDto billDto);

    List<BillDto> toDtoList(List<Bill> bills);

    List<BillItem> toEntityList(List<BillItemDto> billItems);
}