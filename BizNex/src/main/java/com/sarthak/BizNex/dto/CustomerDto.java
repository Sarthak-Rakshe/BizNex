package com.sarthak.BizNex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerContact;
    private String customerAddress;
    private String customerRegistrationDate; // Date when the customer was registered
    @Builder.Default
    private String customerActiveStatus = "active"; // Status of the customer (e.g., "active", "inactive")
    @Builder.Default
    private Double customerCredits = 0.0; // Credits available for the customer (nullable for patch)
}
