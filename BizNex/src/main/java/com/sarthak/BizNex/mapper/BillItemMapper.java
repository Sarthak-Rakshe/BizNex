package com.sarthak.BizNex.mapper;

import com.sarthak.BizNex.dto.BillItemDto;
import com.sarthak.BizNex.entity.BillItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface BillItemMapper {
    BillItemMapper INSTANCE = Mappers.getMapper(BillItemMapper.class);

    @Mapping(target = "billItemProduct", source = "billItemProduct")
    BillItemDto toDto(BillItem billItem);

    // Ignore generated id and parent back-reference when creating entity from DTO
    @Mapping(target = "billItemId", ignore = true)
    @Mapping(target = "bill", ignore = true)
    BillItem toEntity(BillItemDto billItemDto);

    @Mapping(target = "billItemProduct", source = "billItemProduct")
    List<BillItemDto> toDtoList(List<BillItem> billItems);

}
