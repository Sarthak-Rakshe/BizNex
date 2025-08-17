package com.sarthak.BizNex.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId; // Unique identifier for the customer

    @NotBlank
    @NotNull
    @Column(nullable = false)
    private String customerName; // Name of the customer

    @NotBlank
    @NotNull
    @Column(nullable = false, unique = true)
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be 10 digits")
    private String customerContact; // Contact number of the customer

    @Email
    private String CustomerEmail; // Email address of the customer

    private String CustomerAddress; // Address of the customer

    private LocalDateTime CustomerRegistrationDate; // Date when the customer was registered

    @PrePersist
    private void onCreate() {
        // Ensure registration date is set
        if (this.CustomerRegistrationDate == null) {
            this.CustomerRegistrationDate = LocalDateTime.now();
        }
        // Provide default active status if the incoming mapped DTO had null
        if (this.customerActiveStatus == null || this.customerActiveStatus.isBlank()) {
            this.customerActiveStatus = "active";
        }
    }

    @Column(nullable = false)
    private String customerActiveStatus = "active"; // Status of the customer (e.g., "active", "inactive")

    private double customerCredits;


}
