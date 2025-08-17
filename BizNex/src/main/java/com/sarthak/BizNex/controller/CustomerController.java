package com.sarthak.BizNex.controller;

import com.sarthak.BizNex.dto.CustomerDto;
import com.sarthak.BizNex.dto.response.PageResponseDto;
import com.sarthak.BizNex.dto.response.CustomerCreditsPageResponseDto;
import com.sarthak.BizNex.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for customer CRUD, contact-based lookups, and credits queries
 * (both paged and non-paged). Delegates business logic to CustomerService.
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer management with default alphabetical sorting and credits tracking")
public class CustomerController {
    CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    private Pageable buildPageable(int page, int size, String sort){
        if (size<=0) size=20;
        if (page<0) page=0;
        String[] parts = sort.split(",");
        String field = parts.length>0?parts[0]:"customerId";
        Sort.Direction dir = (parts.length>1 && parts[1].equalsIgnoreCase("desc"))? Sort.Direction.DESC: Sort.Direction.ASC;
        return PageRequest.of(page,size, Sort.by(dir, field));
    }

    /** Paged retrieval of all customers. */
    @GetMapping()
    @Operation(summary = "List customers (paged)", description = "Default sorting is by customerName ASC when sort is unspecified or set to customerId. Use sort=field,dir to override (e.g., customerId,desc).")
    public ResponseEntity<PageResponseDto<CustomerDto>> getAllCustomers(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(defaultValue = "customerId,asc") String sort) {
        Pageable pageable = buildPageable(page,size,sort);
        Page<CustomerDto> dtoPage = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }

    /** Search customers across name/contact/email (paged). */
    @GetMapping("/search")
    @Operation(summary = "Search customers (paged)", description = "Case-insensitive contains match across name, contact, email. Empty query returns normal list with default alphabetical sorting.")
    public ResponseEntity<PageResponseDto<CustomerDto>> searchCustomers(@RequestParam String query,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(defaultValue = "customerId,asc") String sort){
        Pageable pageable = buildPageable(page,size,sort);
        Page<CustomerDto> dtoPage = customerService.searchCustomers(query, pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }

    /** Search customers with credits > 0 (paged). */
    @GetMapping("/credits/search")
    @Operation(summary = "Search credit customers (paged)", description = "Filters customers with credits > 0 then applies case-insensitive contains search across name/contact/email. Empty query returns all credit customers (paged).")
    public ResponseEntity<PageResponseDto<CustomerDto>> searchCustomersWithCredits(@RequestParam String query,
                                                                                   @RequestParam(defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "20") int size,
                                                                                   @RequestParam(defaultValue = "customerId,asc") String sort){
        Pageable pageable = buildPageable(page,size,sort);
        Page<CustomerDto> dtoPage = customerService.searchCustomersWithCredits(query, pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }

    /** Retrieve customer by numeric id. */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id)); // throws if not found
    }

    /** Delete a customer by contact number returning deleted representation. */
    @DeleteMapping("/{contact}")
    public ResponseEntity<CustomerDto> deleteCustomerById(@PathVariable String contact) {
        return ResponseEntity.ok(customerService.deleteCustomerByContact(contact));
    }

    /** Create a new customer. */
    @PostMapping()
    public ResponseEntity<CustomerDto> addCustomer(@RequestBody @Valid CustomerDto customerDto) {
        return ResponseEntity.ok(customerService.addCustomer(customerDto));
    }

    /** Update existing customer (partial-like semantics). */
    @PutMapping()
    @Operation(summary = "Update customer", description = "Partial-style update. If customerCredits is decreased, a creditsPayment bill is automatically generated for the reduced amount. Increasing credits does not create a bill.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerDto> updateCustomer(@RequestBody CustomerDto customerDto) {
        return ResponseEntity.ok(customerService.updateCustomer(customerDto));
    }

    /** Find customer by contact number. */
    @GetMapping("/contact/{contact}")
    public ResponseEntity<CustomerDto> getCustomerByContact(@PathVariable String contact) {
        return ResponseEntity.ok(customerService.getCustomerByContact(contact));
    }

    /** Paged list of customers with positive credits. */
    @GetMapping("/credits")
    @Operation(summary = "Customers with credits (paged)", description = "Returns customers with credits > 0 plus aggregate totalCredits & averageCredits across ALL customers with credits > 0 (not just current page). Default sort: customerName ASC unless custom sort param provided.")
    public ResponseEntity<CustomerCreditsPageResponseDto<CustomerDto>> getCustomersWithCreditsPaged(@RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "20") int size,
                                                                                      @RequestParam(defaultValue = "customerId,asc") String sort){
        Pageable pageable = buildPageable(page,size,sort);
        Page<CustomerDto> dtoPage = customerService.getCustomersWithCredits(pageable);
        double total = customerService.totalPositiveCredits();
        double avg = customerService.averagePositiveCredits();
        return ResponseEntity.ok(CustomerCreditsPageResponseDto.from(dtoPage, total, avg));
    }


}
