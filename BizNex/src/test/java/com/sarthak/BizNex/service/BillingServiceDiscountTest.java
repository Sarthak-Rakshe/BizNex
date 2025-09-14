package com.sarthak.BizNex.service;

import com.sarthak.BizNex.dto.BillDto;
import com.sarthak.BizNex.dto.BillItemDto;
import com.sarthak.BizNex.dto.CustomerDto;
import com.sarthak.BizNex.dto.ProductDto;
import com.sarthak.BizNex.dto.response.BillResponseDto;
import com.sarthak.BizNex.entity.Bill;
import com.sarthak.BizNex.entity.Customer;
import com.sarthak.BizNex.entity.Product;
import com.sarthak.BizNex.repository.CustomerRepository;
import com.sarthak.BizNex.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BillingServiceDiscountTest {

    @Autowired
    private BillingService billingService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setup() {
        // Ensure unique values per test to avoid unique-constraint violations
        long now = System.nanoTime();
        String uniqueContact = String.format("%010d", now % 1_000_000_0000L);
        String uniqueCode = "WID-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // fresh customer
        Customer c = new Customer();
        c.setCustomerName("Alice Doe");
        c.setCustomerContact(uniqueContact);
        c.setCustomerCredits(0.0);
        customer = customerRepository.save(c);

        // fresh product
        Product p = new Product();
        p.setProductName("Widget");
        p.setProductDescription("A test widget");
        p.setPricePerItem(100.0);
        p.setProductQuantity(100);
        p.setProductCategory("test");
        p.setProductCode(uniqueCode);
        product = productRepository.save(p);
    }

    @Test
    void discount_is_applied_per_unit_quantity() {
        // Given: qty=3, price=100, discountPerUnit=5
        int qty = 3;
        double price = 100.0;
        double discountPerUnit = 5.0;
        double expectedItemTotal = (price * qty) - (discountPerUnit * qty);
        double expectedTotalDiscount = discountPerUnit * qty;

        // Build bill request
        ProductDto productDto = ProductDto.builder().productId(product.getProductId()).build();
        BillItemDto itemDto = BillItemDto.builder()
                .billItemProduct(productDto)
                .billItemQuantity(qty)
                .billItemDiscountPerUnit(discountPerUnit)
                .build();

        CustomerDto customerDto = CustomerDto.builder().customerId(customer.getCustomerId()).build();
        BillDto billDto = BillDto.builder()
                .customer(customerDto)
                .billItems(List.of(itemDto))
                .paymentMethod(Bill.PaymentMethod.CASH)
                .billStatus(Bill.BillStatus.COMPLETE)
                .build();

        // When
        BillResponseDto response = billingService.createBill(billDto);

        // Then
        assertThat(response.getBillItems()).hasSize(1);
        BillResponseDto.BillItemResponseDto respItem = response.getBillItems().get(0);
        assertThat(respItem.getBillItemQuantity()).isEqualTo(qty);
        assertThat(respItem.getBillItemPricePerUnit()).isEqualTo(price);
        assertThat(respItem.getDiscountPerUnit()).isEqualTo(discountPerUnit);

        // item total should subtract discount per unit times quantity
        assertThat(respItem.getTotalPrice()).isEqualTo(expectedItemTotal);

        // bill totals should align
        assertThat(response.getTotalDiscount()).isEqualTo(expectedTotalDiscount);
        assertThat(response.getTotalAmount()).isEqualTo(expectedItemTotal);
    }

    @Test
    void discount_is_consistent_across_multiple_items() {
        // Create two additional products with specific prices
        Product p1 = new Product();
        p1.setProductName("Gadget");
        p1.setProductDescription("Gadget desc");
        p1.setPricePerItem(50.0);
        p1.setProductQuantity(200);
        p1.setProductCategory("test");
        p1.setProductCode("GAD-050");
        p1 = productRepository.save(p1);

        Product p2 = new Product();
        p2.setProductName("Thing");
        p2.setProductDescription("Thing desc");
        p2.setPricePerItem(20.0);
        p2.setProductQuantity(300);
        p2.setProductCategory("test");
        p2.setProductCode("THN-020");
        p2 = productRepository.save(p2);

        int qty1 = 2; double price1 = 50.0; double d1 = 3.0; // expected total: 50*2 - 3*2 = 94
        int qty2 = 5; double price2 = 20.0; double d2 = 0.5; // expected total: 20*5 - 0.5*5 = 97.5

        double expectedTotalDiscount = (d1 * qty1) + (d2 * qty2); // 8.5
        double expectedGrandTotal = (price1 * qty1 - d1 * qty1) + (price2 * qty2 - d2 * qty2); // 191.5

        BillItemDto item1 = BillItemDto.builder()
                .billItemProduct(ProductDto.builder().productId(p1.getProductId()).build())
                .billItemQuantity(qty1)
                .billItemDiscountPerUnit(d1)
                .build();

        BillItemDto item2 = BillItemDto.builder()
                .billItemProduct(ProductDto.builder().productId(p2.getProductId()).build())
                .billItemQuantity(qty2)
                .billItemDiscountPerUnit(d2)
                .build();

        BillDto billDto = BillDto.builder()
                .customer(CustomerDto.builder().customerId(customer.getCustomerId()).build())
                .billItems(List.of(item1, item2))
                .paymentMethod(Bill.PaymentMethod.CASH)
                .billStatus(Bill.BillStatus.COMPLETE)
                .build();

        BillResponseDto response = billingService.createBill(billDto);

        assertThat(response.getBillItems()).hasSize(2);
        assertThat(response.getTotalDiscount()).isEqualTo(expectedTotalDiscount);
        assertThat(response.getTotalAmount()).isEqualTo(expectedGrandTotal);

        // verify per-item discount per unit echoed back correctly
        assertThat(response.getBillItems().stream().anyMatch(i -> i.getProductName().equals("Gadget") && i.getDiscountPerUnit() == d1)).isTrue();
        assertThat(response.getBillItems().stream().anyMatch(i -> i.getProductName().equals("Thing") && i.getDiscountPerUnit() == d2)).isTrue();
    }
}
