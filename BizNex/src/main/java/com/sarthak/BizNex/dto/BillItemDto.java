package com.sarthak.BizNex.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItemDto {

    // Accept both "billItemProduct" and shorthand "product" from frontend payload
    @JsonProperty("billItemProduct")
    @JsonAlias({"product"})
    @NotNull(message = "Product reference cannot be null for a bill item")
    private ProductDto billItemProduct;


    private int billItemQuantity;

    private double pricePerUnit; // Will be overwritten from DB product price for integrity
    private double billItemTotalPrice; // Calculated server-side (pricePerUnit * billItemQuantity - discount if any)
    private double billItemDiscountPerUnit; // Optional: discount per item

}
