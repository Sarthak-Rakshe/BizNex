package com.sarthak.BizNex.entity;

import com.sarthak.BizNex.exception.BillInformationInvalidException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "bill_items")
public class BillItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product billItemProduct;

    @Min(1)
    @Column(nullable = false)
    private int billItemQuantity;


    @Column(nullable = false)
    private double pricePerUnit;

    private double billItemTotalPrice; // Optional: total price for the item

    private double billItemDiscountPerUnit; // Optional: discount per item


    public double getTotal() {
        // Apply discount per unit for the entire quantity
        return (pricePerUnit * billItemQuantity) - (billItemDiscountPerUnit * billItemQuantity);
    }

    public double getTotalDiscount() {
        return billItemDiscountPerUnit * billItemQuantity;
    }

    @PrePersist
    public void prePersist() {
        //Calculate total price and discount before persisting
        if (billItemProduct != null) {
            this.pricePerUnit = billItemProduct.getPricePerItem();
            this.billItemTotalPrice = getTotal();
        } else {
            throw new BillInformationInvalidException("Bill item must have a valid product.");
        }
    }

    @PreUpdate
    public void preUpdate() {
        // Ensure that the bill item has a valid quantity and product before updating
        if(billItemProduct != null){
            this.pricePerUnit = billItemProduct.getPricePerItem();
            this.billItemTotalPrice = getTotal(); // Recalculate total price
        }else{
            throw new BillInformationInvalidException("Bill item must have a valid product.");
        }
    }

    @Override
    public String toString() {
        return "BillItem{" +
                "billItemId=" + billItemId +
                ", productId=" + (billItemProduct != null ? billItemProduct.getProductId() : null) +
                ", quantity=" + billItemQuantity +
                '}';
    }

}
