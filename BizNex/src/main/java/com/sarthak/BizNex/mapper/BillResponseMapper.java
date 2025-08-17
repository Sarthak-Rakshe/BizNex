package com.sarthak.BizNex.mapper;

import com.sarthak.BizNex.dto.response.BillResponseDto;
import com.sarthak.BizNex.entity.Bill;
import com.sarthak.BizNex.entity.BillItem;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BillResponseMapper {
    BillResponseMapper INSTANCE = Mappers.getMapper(BillResponseMapper.class);

    @Mappings({
            @Mapping(target = "customerName", source = "customer.customerName"),
            @Mapping(target = "customerEmail", source = "customer.customerEmail"),
            @Mapping(target = "customerPhone", source = "customer.customerContact"),
            @Mapping(target = "billDate", expression = "java(formatDate(bill.getBillDate()))"),
            @Mapping(target = "totalAmount", source = "billTotalAmount"),
            @Mapping(target = "totalDiscount", source = "billTotalDiscount"),
            @Mapping(target = "billType", source = "billType"),
            @Mapping(target = "billStatus", source = "billStatus"),
            @Mapping(target = "paymentMethod", source = "paymentMethod"),
            @Mapping(target = "originalBillNumber", source = "originalBillNumber"),
            @Mapping(target = "billItems", expression = "java(mapBillItems(bill.getBillItems()))")
    })
    BillResponseDto toResponseDto(Bill bill);

    default String formatDate(java.time.LocalDateTime date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    default List<BillResponseDto.BillItemResponseDto> mapBillItems(List<BillItem> items) {
        if (items == null) return null;
        return items.stream().map(item -> {
            BillResponseDto.BillItemResponseDto dto = new BillResponseDto.BillItemResponseDto();
            dto.setProductId(item.getBillItemProduct().getProductId());
            dto.setProductName(item.getBillItemProduct().getProductName());
            dto.setBillItemQuantity(item.getBillItemQuantity());
            dto.setBillItemPricePerUnit(item.getPricePerUnit());
            dto.setDiscountPerUnit(item.getBillItemDiscountPerUnit());
            dto.setTotalPrice(item.getTotal());
            return dto;
        }).collect(Collectors.toList());
    }

    List<BillResponseDto> toResponseDtoList(List<Bill> bills);
}