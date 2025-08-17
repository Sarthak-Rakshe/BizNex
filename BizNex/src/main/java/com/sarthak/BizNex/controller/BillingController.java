package com.sarthak.BizNex.controller;

import com.sarthak.BizNex.dto.BillDto;
import com.sarthak.BizNex.dto.response.BillResponseDto;
import com.sarthak.BizNex.dto.response.PageResponseDto;
import com.sarthak.BizNex.service.BillingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Billing REST endpoints for creating bills, handling returns / credit notes,
 * fetching bills (paged & non-paged) and deleting bills.
 * All business rules are delegated to BillingService.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController (BillingService billingService) {
        this.billingService = billingService;
    }

    /** Build a Pageable from request parameters with defensive defaults. */
    private Pageable buildPageable(int page, int size, String sort){
        if(size<=0) size=20; if(page<0) page=0;
        String[] parts = sort.split(",");
        String field = parts.length>0?parts[0]:"billId"; // assuming billId field exists
        Sort.Direction dir = (parts.length>1 && parts[1].equalsIgnoreCase("desc"))? Sort.Direction.DESC: Sort.Direction.ASC;
        return PageRequest.of(page,size, Sort.by(dir, field));
    }

    /** Create a new bill (standard sale or credit depending on DTO fields). */
    @PostMapping()
    public ResponseEntity<BillResponseDto> createBill(@RequestBody @NotNull @Valid BillDto billDto) {
        return ResponseEntity.ok(billingService.createBill(billDto));
    }

    /** Retrieve a bill by its unique bill number. */
    @GetMapping("/{billNumber}")
    public ResponseEntity<BillResponseDto> getBillByBillNumber(@PathVariable String billNumber) {
        return ResponseEntity.ok(billingService.getBillByBillNumber(billNumber)); // throws if not found
    }

    /** Bill search across billNumber, customerName/contact, billType, paymentMethod, originalBillNumber (paged). */
    @GetMapping("/search")
    public ResponseEntity<PageResponseDto<BillResponseDto>> searchBills(@RequestParam String query,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(defaultValue = "billDate,desc") String sort){
        Pageable pageable = buildPageable(page,size,sort);
        Page<BillResponseDto> dtoPage = billingService.searchBills(query, pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }

    /** Process a full or partial return for an existing bill. */
    @PostMapping("/return-bill")
    public ResponseEntity<BillResponseDto> updateBill(@RequestBody BillDto billDto) {
        return ResponseEntity.ok(billingService.updateBillForReturn(billDto));
    }

    /** Apply a customer credit payment bill entry. */
    @PostMapping("/credit-bill")
    public ResponseEntity<BillResponseDto> creditBill(@RequestBody BillDto billDto) {
        return ResponseEntity.ok(billingService.createCreditBill(billDto));
    }

    /** Paged listing of all bills. */
    @GetMapping()
    public ResponseEntity<PageResponseDto<BillResponseDto>> getAllBills(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(defaultValue = "billId,asc") String sort) {
        Pageable pageable = buildPageable(page,size,sort);
        Page<BillResponseDto> dtoPage = billingService.getAllBills(pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }


    /** Paged listing of bills for a given customer contact. */
    @GetMapping("/customer/{contact}")
    public ResponseEntity<PageResponseDto<BillResponseDto>> getBillsByCustomerContactPaged(@PathVariable String contact,
                                                                                           @RequestParam(defaultValue = "0") int page,
                                                                                           @RequestParam(defaultValue = "20") int size,
                                                                                           @RequestParam(defaultValue = "billId,asc") String sort){
        Pageable pageable = buildPageable(page,size,sort);
        Page<BillResponseDto> dtoPage = billingService.getBillsByCustomerContact(contact, pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }

    /** Delete a bill by id (idempotent-like; throws if not found). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBill(@PathVariable Long id) {
        billingService.deleteBillById(id); // throws if not found
        return ResponseEntity.noContent().build();
    }
}
