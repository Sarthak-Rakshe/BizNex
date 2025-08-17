package com.sarthak.BizNex.mapper;

import com.sarthak.BizNex.dto.ProductDto;
import com.sarthak.BizNex.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);
    ProductDto toDto(Product product);
    Product toEntity(ProductDto productDto);

    List<ProductDto> toDtoList(List<Product> all);
}

