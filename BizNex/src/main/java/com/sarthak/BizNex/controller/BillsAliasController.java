package com.sarthak.BizNex.controller;

import com.sarthak.BizNex.dto.response.BillResponseDto;
import com.sarthak.BizNex.dto.response.PageResponseDto;
import com.sarthak.BizNex.service.BillingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Alias controller to expose bill search under /api/v1/bills to align with documented endpoint.
 * Primary bill operations remain under /api/v1/billing for backward compatibility.
 */
@RestController
@RequestMapping("/api/v1/bills")
public class BillsAliasController {

    private final BillingService billingService;

    public BillsAliasController(BillingService billingService) {
        this.billingService = billingService;
    }

    private Pageable buildPageable(int page, int size, String sort){
        if(size<=0) size=20; if(page<0) page=0;
        String[] parts = sort.split(",");
        String field = parts.length>0?parts[0]:"billDate";
        Sort.Direction dir = (parts.length>1 && parts[1].equalsIgnoreCase("desc"))? Sort.Direction.DESC: Sort.Direction.ASC;
        return PageRequest.of(page,size, Sort.by(dir, field));
    }

    /** Search bills (alias). */
    @GetMapping("/search")
    public ResponseEntity<PageResponseDto<BillResponseDto>> searchBills(@RequestParam String query,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(defaultValue = "billDate,desc") String sort){
        Pageable pageable = buildPageable(page,size,sort);
        Page<BillResponseDto> dtoPage = billingService.searchBills(query, pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }
}

