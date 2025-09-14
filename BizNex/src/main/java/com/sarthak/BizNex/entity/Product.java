package com.sarthak.BizNex.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @NotBlank
    @NotNull
    @Column(nullable = false)
    private String productName;

    private String productDescription;

    @NotNull
    @Column(nullable = false)
    private double pricePerItem;

    @NotNull
    @Column(nullable = false)
    private double productTotalPrice;

    @NotNull
    @Column(nullable = false)
    private int productQuantity;

    @NotNull
    private String productCategory; // e.g., "electronics", "furniture", etc.

    @Column(nullable = false, unique = true)
    private String productCode; // Unique code for the product

    // Soft-delete flag: active products are visible/selectable; inactive remain for historical linkage
    @Column(nullable = false)
    private boolean productActive = true;

    @PrePersist
    public void prePersist() {
        // Ensure that the product has a valid name and price before saving
        productTotalPrice = pricePerItem * productQuantity;

        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank.");
        }
        if (pricePerItem <= 0) {
            throw new IllegalArgumentException("Product price must be greater than zero.");
        }
        if (productQuantity < 0) {
            throw new IllegalArgumentException("Product quantity cannot be negative.");
        }
        // default active already true
    }

    @PreUpdate
    public void preUpdate() {
        // Update total price on update
        productTotalPrice = pricePerItem * productQuantity;

        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank.");
        }
        if (pricePerItem <= 0) {
            throw new IllegalArgumentException("Product price must be greater than zero.");
        }
        if (productQuantity < 0) {
            throw new IllegalArgumentException("Product quantity cannot be negative.");
        }
    }
}
