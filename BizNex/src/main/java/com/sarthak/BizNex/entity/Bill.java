package com.sarthak.BizNex.entity;


import com.sarthak.BizNex.exception.BillInformationInvalidException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "bills")
public class Bill {
    
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long billId;
    

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillItem> billItems; // List of bill items in the bill

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer; // Customer associated with the bill

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillType billType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillStatus billStatus; // Status of the bill (e.g., COMPLETE, CANCELLED, RETURNED)

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod; // Method of payment

    private double billTotalAmount=0; // Total amount of the bill

    private double billTotalDiscount; // Total discount applied to the bill

    @Column(nullable = false, unique = true)
    private String billNumber; // Unique identifier for the bill

    @Column(nullable = false)
    private LocalDateTime billDate; // Date of the bill

    @Column(nullable = false)
    private String originalBillNumber = "NA"; // Original bill number for returns or credits

   private String generateBillNumber() {
        // Generate bill number: 2 letters (customer initials) + 6 digits (ddMMyy) + 4 random alphanumerics
        String initials = "XX";
        if (customer != null && customer.getCustomerName() != null) {
            String[] nameParts = customer.getCustomerName().split(" ");
            String firstInitial = nameParts.length > 0 && !nameParts[0].isEmpty() ? nameParts[0].substring(0, 1).toUpperCase() : "X";
            String lastInitial = nameParts.length > 1 && !nameParts[1].isEmpty() ? nameParts[1].substring(0, 1).toUpperCase() : "X";
            initials = firstInitial + lastInitial;
        }
        LocalDateTime now = LocalDateTime.now();
        String dateTimePart = String.format("%02d%02d%02d", now.getDayOfMonth(), now.getMonthValue(), now.getYear() % 100);
        String randomPart = java.util.UUID.randomUUID().toString().replaceAll("[^A-Za-z0-9]", "").substring(0, 4).toUpperCase();
        return initials + dateTimePart + "-" + randomPart;
    }

    // Combine all @PrePersist logic into one method
    @PrePersist
    public void prePersist() {
        // Provide safe defaults if omitted by client/DTO builder
        if (this.originalBillNumber == null || this.originalBillNumber.isBlank()) {
            this.originalBillNumber = "NA";
        }
        if (this.billType == null) {
            this.billType = BillType.NEW; // default for freshly created sales bill
        }

        // Calculate the total amount of the bill
        if (billItems != null && !billItems.isEmpty() && billTotalAmount==0 ) {
            billTotalAmount = billItems.stream()
                    .mapToDouble(BillItem::getTotal)
                    .sum();
        }

        //Logic to calculate total discount if applicable
        if (billItems != null && !billItems.isEmpty()) {
            billTotalDiscount = billItems.stream()
                    .mapToDouble(BillItem::getTotalDiscount)
                    .sum();
        } else {
            billTotalDiscount = 0.0; // No items, no discount
        }

        // Set the bill date to the current date when the bill is created
        this.billDate = LocalDateTime.now().withNano(0);
        // Generate a unique bill number
        this.billNumber = generateBillNumber();
        // Validate customer
        if (customer == null) {
            throw new BillInformationInvalidException("Bill must be associated with a customer.");
        }
        // Validate bill status & payment method
        if (billStatus == null) {
            throw new BillInformationInvalidException("Bill status must not be null.");
        }
        if (paymentMethod == null) {
            throw new BillInformationInvalidException("Payment method must not be null.");
        }
    }

    // Combine all @PreUpdate logic into one method
    @PreUpdate
    public void preUpdate() {

        // Validate customer
        if (customer == null) {
            throw new BillInformationInvalidException("Bill must be associated with a customer.");
        }

        // Calculate the total amount of the bill
        if (billItems != null && !billItems.isEmpty() && billTotalAmount==0) {
            billTotalAmount = billItems.stream()
                    .mapToDouble(BillItem::getTotal)
                    .sum();
        }

        if (billItems != null && !billItems.isEmpty()) {
            billTotalDiscount = billItems.stream()
                    .mapToDouble(BillItem::getTotalDiscount)
                    .sum();
        } else {
            billTotalDiscount = 0.0; // No items, no discount
        }

        // Validate bill status
        if (billStatus == null) {
            throw new BillInformationInvalidException("Bill status must not be null.");
        }
        if (billType == null) {
            throw new BillInformationInvalidException("Bill type must not be null.");
        }
        if (paymentMethod == null) {
            throw new BillInformationInvalidException("Payment method must not be null.");
        }
    }

    @Override
    public String toString() {
        return "Bill{" +
                "billId=" + billId +
                ", billNumber='" + billNumber + '\'' +
                ", customerId=" + (customer != null ? customer.getCustomerId() : null) +
                ", billItemIds=" + (billItems != null ? billItems.stream().map(BillItem::getBillItemId).toList() : null) +
                ", billType='" + billType + '\'' +
                ", billStatus='" + billStatus + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", billTotalAmount=" + billTotalAmount +
                ", billTotalDiscount=" + billTotalDiscount +
                ", billDate=" + billDate +
                ", originalBillNumber='" + originalBillNumber + '\'' +
                '}';
    }

    public enum BillType {
        NEW,
        CREDITS_PAYMENT,
        PARTIAL_RETURN,
        FULL_RETURN
    }

    public enum BillStatus {
        COMPLETE,
        CANCELLED,
        RETURNED
    }

    public enum PaymentMethod {
        CASH,
        ONLINE,
        CREDIT,
        CARD
    }
}
