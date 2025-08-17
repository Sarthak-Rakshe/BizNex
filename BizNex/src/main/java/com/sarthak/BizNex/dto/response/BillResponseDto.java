package com.sarthak.BizNex.dto.response;

import lombok.Data;

import java.util.List;

@Data

public class BillResponseDto {
    //write logic for BillResponseDto
    private String billNumber;
    private String billDate;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String billType; // e.g., "New", "Return", "Credit"
    private List<BillItemResponseDto> billItems; // JSON or string representation of bill items
    private String paymentMethod; // e.g., "Cash", "Credit Card", "Debit Card", "Online"
    private double totalAmount;
    private double totalDiscount;
    private String billStatus; // e.g., "Paid", "Pending", "Refunded"
    private String originalBillNumber; // For returns or credits, the original bill


    @Data
    public static class BillItemResponseDto {
        private Long productId;
        private String productName;
        private double billItemPricePerUnit;
        private int billItemQuantity;
        private double discountPerUnit;
        private double totalPrice;
    }
}
