package com.sarthak.BizNex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillDto {

    private String billNumber;
    private CustomerDto customer;
    @Builder.Default
    private String billType = "new";
    private List<BillItemDto> billItems;
    private String paymentMethod;
    private String billDate;
    private double billTotalAmount;
    private double billTotalDiscount;
    private String billStatus;
    @Builder.Default
    private String originalBillNumber = "NA";

}
