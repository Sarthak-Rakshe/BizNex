package com.sarthak.BizNex.dto;

import com.sarthak.BizNex.entity.Bill;
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
    private Bill.BillType billType = Bill.BillType.NEW;
    private List<BillItemDto> billItems;
    private Bill.PaymentMethod paymentMethod;
    private String billDate;
    private double billTotalAmount;
    private double billTotalDiscount;
    private Bill.BillStatus billStatus;
    @Builder.Default
    private String originalBillNumber = "NA";

}
