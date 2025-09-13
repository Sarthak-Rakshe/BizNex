package com.sarthak.BizNex.dto.response;

import com.sarthak.BizNex.entity.Bill;
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
    private Bill.BillType billType; // enum
    private List<BillItemResponseDto> billItems; // JSON or string representation of bill items
    private Bill.PaymentMethod paymentMethod; // enum
    private double totalAmount;
    private double totalDiscount;
    private Bill.BillStatus billStatus; // enum
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
