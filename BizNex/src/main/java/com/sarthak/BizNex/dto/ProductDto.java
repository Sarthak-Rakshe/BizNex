package com.sarthak.BizNex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {
    private Long productId; // Unique identifier for the product
    @NotBlank(message = "Product name is required")
    private String productName;
    private String productDescription;
    @NotNull(message = "Price per item is required")
    private Double pricePerItem;
    private Double productTotalPrice; // Total price calculated as pricePerItem * quantity (read-only on output)
    @NotNull(message = "Quantity is required")
    private Integer productQuantity; // Quantity of the product
    @NotBlank(message = "Product category is required")
    private String productCategory; // e.g., "electronics", "furniture", etc.
    private String productCode; // Unique code for the product
}
