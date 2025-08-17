package com.sarthak.BizNex.service;

import com.sarthak.BizNex.dto.BillDto;
import com.sarthak.BizNex.dto.BillItemDto;
import com.sarthak.BizNex.dto.response.BillResponseDto;
import com.sarthak.BizNex.entity.Bill;
import com.sarthak.BizNex.entity.BillItem;
import com.sarthak.BizNex.entity.Customer;
import com.sarthak.BizNex.entity.Product;
import com.sarthak.BizNex.exception.*;
import com.sarthak.BizNex.mapper.BillItemMapper;
import com.sarthak.BizNex.mapper.BillMapper;
import com.sarthak.BizNex.mapper.BillResponseMapper;
import com.sarthak.BizNex.repository.BillRepository;
import com.sarthak.BizNex.repository.CustomerRepository;
import com.sarthak.BizNex.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Billing domain service handling bill creation, returns (partial / full), credit adjustments,
 * pagination, and linkage between original bills and return/credit notes.
 */
@Service
public class BillingService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final BillMapper billMapper;
    private final BillItemMapper billItemMapper;
    private final BillResponseMapper billResponseMapper;


    public BillingService( BillMapper billMapper, BillItemMapper billItemMapper,
                          CustomerRepository customerRepository,
                          ProductRepository productRepository,
                           BillRepository billRepository, BillResponseMapper billResponseMapper) {
        this.billMapper = billMapper;
        this.billItemMapper = billItemMapper;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.billRepository = billRepository;
        this.billResponseMapper = billResponseMapper;
    }


    /** Create a new bill (validates customer, items, stock, and updates credits if paymentMethod=credit). */
    @Transactional
    public BillResponseDto createBill(BillDto billDto) {
        if (billDto.getCustomer() == null || billDto.getCustomer().getCustomerId() == null) {
            throw new CustomerInformationMissingInBillException("Customer information is missing in the bill request");
        }
        if (billDto.getBillItems() == null || billDto.getBillItems().isEmpty()) {
            throw new BillInformationInvalidException("Bill must contain at least one bill item");
        }
        Customer customer = customerRepository.findById(billDto.getCustomer().getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        List<BillItem> billItems = new ArrayList<>();

        int index = 0;
        for (BillItemDto itemDto : billDto.getBillItems()) {
            if (itemDto == null) {
                throw new BillInformationInvalidException("Bill item at index " + index + " is null");
            }
            if (itemDto.getBillItemProduct() == null || itemDto.getBillItemProduct().getProductId() == null) {
                throw new BillInformationInvalidException("Product reference missing for bill item at index " + index + ". Ensure JSON contains 'product': {'productId': <id>} ");
            }
            Long productId = itemDto.getBillItemProduct().getProductId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found (id=" + productId + ")"));
            if (product.getProductQuantity() < itemDto.getBillItemQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getProductName());
            }
            if (itemDto.getBillItemQuantity() <= 0) {
                throw new BillInformationInvalidException("Quantity must be positive for product id=" + productId);
            }
            // Subtract the quantity from product stock (will be persisted later in same transaction)
            product.setProductQuantity(product.getProductQuantity() - itemDto.getBillItemQuantity());
            productRepository.save(product);
            // Map and override authoritative fields
            BillItem billItem = billItemMapper.toEntity(itemDto);
            billItem.setBillItemProduct(product); // ensure managed entity
            billItem.setPricePerUnit(product.getPricePerItem()); // authoritative price
            billItems.add(billItem);
            index++;
        }
        Bill bill = billMapper.toEntity(billDto);
        bill.setCustomer(customer);
        bill.setBillType("new");
        bill.setBillItems(billItems);
        for (BillItem item : billItems) {
            item.setBill(bill);
        }
        Bill savedBill = billRepository.save(bill);
        // Update customer credit after bill is saved and total is calculated
        if (billDto.getPaymentMethod() != null && billDto.getPaymentMethod().equalsIgnoreCase("credit")) {
            double currentCredit = customer.getCustomerCredits();
            customer.setCustomerCredits(currentCredit + savedBill.getBillTotalAmount());
            customerRepository.save(customer);
        }
        return billResponseMapper.toResponseDto(savedBill);
    }

    // Retrieves a bill by its bill number

    /** Retrieve a bill by billNumber or throw EntityNotFoundException. */
    public BillResponseDto getBillByBillNumber(String billNumber) {
        Bill bill = billRepository.findByBillNumberIgnoreCase(billNumber)
                .orElseThrow(() -> new EntityNotFoundException("Bill not found"));
        return billResponseMapper.toResponseDto(bill);
    }


    // Updates a bill for return (full-return or partial-return) based on billDto.billType
    /** Process a return bill (partial or full) updating inventory and customer credits accordingly. */
    @Transactional
    public BillResponseDto updateBillForReturn(BillDto billDto) {

        if (billDto.getBillNumber() == null || billDto.getBillNumber().isEmpty()) {
            throw new InvalidBillReturnException("Bill number for return cannot be null or empty");
        }
        if (billDto.getBillItems() == null || billDto.getBillItems().isEmpty()) {
            throw new InvalidBillReturnException("Bill items for return cannot be null or empty");
        }
        String requestedType = billDto.getBillType();
        if (requestedType == null || requestedType.isBlank()) {
            throw new InvalidBillReturnException("billType must be provided as 'full-return' or 'partial-return' " +
                    "current provided: " + requestedType);
        }

        Bill originalBill = billRepository.findByBillNumberIgnoreCase(billDto.getBillNumber())
                .orElseThrow(() -> new EntityNotFoundException("Bill not found"));

        if (originalBill.getBillType().equalsIgnoreCase("full-return")) {
            throw new InvalidBillReturnException("This bill has already been fully returned");
        }

        // Gather previous return bills (partial or full) for this original bill
        List<Bill> priorReturnBills = billRepository.findByOriginalBillNumberIgnoreCase(originalBill.getBillNumber());
        // Aggregate already returned quantities per product
        Map<Long, Integer> alreadyReturnedQty = new HashMap<>();
        for (Bill rBill : priorReturnBills) {
            if (rBill.getBillItems() == null) continue;
            for (BillItem ri : rBill.getBillItems()) {
                Long pid = ri.getBillItemProduct().getProductId();
                alreadyReturnedQty.merge(pid, ri.getBillItemQuantity(), Integer::sum);
            }
        }

        // Build original quantities map
        Map<Long, Integer> originalQty = originalBill.getBillItems().stream()
                .collect(Collectors.toMap(bi -> bi.getBillItemProduct().getProductId(), BillItem::getBillItemQuantity, Integer::sum));

        // Compute remaining quantities available for return
        Map<Long, Integer> remainingQty = new HashMap<>();
        for (Map.Entry<Long, Integer> e : originalQty.entrySet()) {
            int returned = alreadyReturnedQty.getOrDefault(e.getKey(), 0);
            int remaining = e.getValue() - returned;
            if (remaining < 0) {
                throw new InvalidBillReturnException("Data inconsistency: returned more than purchased for product id=" + e.getKey());
            }
            if (remaining > 0) {
                remainingQty.put(e.getKey(), remaining);
            }
        }

        if (remainingQty.isEmpty()) {
            throw new InvalidBillReturnException("All items have already been fully returned for this bill");
        }

        // Validate requested return items: cannot exceed remaining
        for (BillItemDto retDto : billDto.getBillItems()) {
            if (retDto.getBillItemProduct() == null || retDto.getBillItemProduct().getProductId() == null) {
                throw new InvalidBillReturnException("Product reference missing in return item");
            }
            Long pid = retDto.getBillItemProduct().getProductId();
            String pName = retDto.getBillItemProduct().getProductName();
            Integer remaining = remainingQty.get(pid);
            if (remaining == null) {
                throw new InvalidBillReturnException("Product " + pName + " not available for return (maybe fully " +
                        "returned already or not in original bill)");
            }
            if (retDto.getBillItemQuantity() <= 0) {
                throw new InvalidBillReturnException("Return quantity must be positive for product id=" + pid);
            }
            if (retDto.getBillItemQuantity() > remaining) {
                throw new InvalidBillReturnException("Return quantity " + retDto.getBillItemQuantity() + " exceeds remaining quantity " + remaining + " for product id=" + pid);
            }
        }

        // Determine if this specific request constitutes a full-return of remaining OR entire original order
        boolean isFullReturnOfOriginal = isFullReturnOriginal(originalQty, alreadyReturnedQty, billDto.getBillItems());

        String processedType;
        if (requestedType.equalsIgnoreCase("full-return")) {
            // Only allowed if EVERYTHING (original) is being returned with this request considering previous partials
            if (!isFullReturnOfOriginal) {
                throw new InvalidBillReturnException("Requested full-return but cumulative returns won't cover entire original bill");
            }
            processedType = "full-return";
        } else if (requestedType.equalsIgnoreCase("partial-return")) {
            processedType = isFullReturnOfOriginal ? "full-return" : "partial-return"; // auto-upgrade if completes the full return
        } else {
            throw new InvalidBillReturnException("Unsupported billType: " + requestedType);
        }

        // Update stock for returned quantities
        for (BillItemDto itemDto : billDto.getBillItems()) {
            Product product = productRepository.findById(itemDto.getBillItemProduct().getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            product.setProductQuantity(product.getProductQuantity() + itemDto.getBillItemQuantity());
            productRepository.save(product);
        }

        // If this call results in a complete full return (whether requested or auto-upgraded), mark original bill accordingly
        if (processedType.equals("full-return")) {
            originalBill.setBillStatus("cancelled");
            originalBill.setBillType("full-return");
            billRepository.save(originalBill);
        } else {
            // Keep original bill type/status intact for partial returns
            if (!originalBill.getBillType().equalsIgnoreCase("partial-return")) {
                originalBill.setBillType("partial-return");
                originalBill.setBillStatus("returned");
                billRepository.save(originalBill);
            }
        }

        // Construct return bill (credit note)
        Bill returnBill = new Bill();
        List<BillItem> returnItemsEntities = new ArrayList<>();
        for (BillItemDto retDto : billDto.getBillItems()) {
            BillItem originalItem = originalBill.getBillItems().stream()
                    .filter(i -> i.getBillItemProduct().getProductId().equals(retDto.getBillItemProduct().getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidBillReturnException("Product not found in original bill for return: " + retDto.getBillItemProduct().getProductId()));
            BillItem ri = new BillItem();
            ri.setBillItemProduct(originalItem.getBillItemProduct());
            ri.setBillItemQuantity(retDto.getBillItemQuantity());
            ri.setPricePerUnit(originalItem.getPricePerUnit());
            ri.setBillItemDiscountPerUnit(originalItem.getBillItemDiscountPerUnit());
            ri.setBill(returnBill);
            returnItemsEntities.add(ri);
        }
        returnBill.setBillItems(returnItemsEntities);
        returnBill.setCustomer(originalBill.getCustomer());
        returnBill.setBillType(processedType);
        returnBill.setBillStatus("complete");
        returnBill.setPaymentMethod(billDto.getPaymentMethod());
        returnBill.setOriginalBillNumber(originalBill.getBillNumber());
        Bill savedReturnBill = billRepository.save(returnBill);

        if (originalBill.getPaymentMethod().equalsIgnoreCase("credit")) {
            Customer updated = adjustCustomerCreditsForReturn(originalBill, savedReturnBill, processedType);
            customerRepository.save(updated);
        }

        return billResponseMapper.toResponseDto(savedReturnBill);
    }

    private Customer adjustCustomerCreditsForReturn(Bill originalBill, Bill savedReturnBill, String processedType) {
        double amountToSubtract;
        Customer customer = originalBill.getCustomer();

        if (processedType.equalsIgnoreCase("full-return")) {
            // Calculate total credits already subtracted for previous partial returns
            double alreadyReturnedCredits = 0.0;
            List<Bill> priorReturnBills = billRepository.findByOriginalBillNumberIgnoreCase(originalBill.getBillNumber());
            for (Bill rBill : priorReturnBills) {
                if (!rBill.getBillType().equalsIgnoreCase("full-return")) {
                    alreadyReturnedCredits += rBill.getBillTotalAmount();
                }
            }
            // Only subtract the remaining amount to reach the original bill total
            amountToSubtract = originalBill.getBillTotalAmount() - alreadyReturnedCredits;
        } else {
            amountToSubtract = savedReturnBill.getBillTotalAmount();
        }

        double updatedCredits = customer.getCustomerCredits() - amountToSubtract;

        if (updatedCredits < 0) {
            throw new InvalidBillReturnException("Customer credits cannot be negative after return");
        }
        customer.setCustomerCredits(updatedCredits);
        return customer;
    }

    private boolean isExactMatch(Map<Long, Integer> remainingQty, List<BillItemDto> returnItems) {
        if (remainingQty.size() != returnItems.size()) return false;
        for (BillItemDto dto : returnItems) {
            Long pid = dto.getBillItemProduct() != null ? dto.getBillItemProduct().getProductId() : null;
            if (pid == null) return false;
            Integer rem = remainingQty.get(pid);
            if (rem == null || rem != dto.getBillItemQuantity()) return false;
        }
        return true;
    }

    private boolean isFullReturnOriginal(Map<Long, Integer> originalQty, Map<Long, Integer> alreadyReturnedQty, List<BillItemDto> newReturnItems) {
        // Combine already returned with this request
        Map<Long, Integer> cumulative = new HashMap<>(alreadyReturnedQty);
        for (BillItemDto dto : newReturnItems) {
            if (dto.getBillItemProduct() == null || dto.getBillItemProduct().getProductId() == null) {
                return false;
            }
            cumulative.merge(dto.getBillItemProduct().getProductId(), dto.getBillItemQuantity(), Integer::sum);
        }
        // Compare against original
        if (cumulative.size() != originalQty.size()) return false;
        for (Map.Entry<Long, Integer> e : originalQty.entrySet()) {
            if (!cumulative.containsKey(e.getKey())) return false;
            if (!cumulative.get(e.getKey()).equals(e.getValue())) return false;
        }
        return true;
    }

    /** Paged list of all bills. */
    public Page<BillResponseDto> getAllBills(Pageable pageable) {
        Page<Bill> page = billRepository.findAll(pageable);
        return page.map(billResponseMapper::toResponseDto);
    }

    /** Non-paged list of all bills (legacy). */
    public List<BillResponseDto> getAllBills() {
        List<Bill> bills = billRepository.findAll();
        return billResponseMapper.toResponseDtoList(bills);
    }

    /** Bills by customer (non-paged). */
    public List<BillResponseDto> getBillsByCustomerContact(String contact) {
        Customer customer = customerRepository.findByCustomerContact(contact)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        List<Bill> bills = billRepository.findByCustomer(customer);
        return billResponseMapper.toResponseDtoList(bills);
    }

    /** Bills by customer (paged). */
    public Page<BillResponseDto> getBillsByCustomerContact(String contact, Pageable pageable) {
        Customer customer = customerRepository.findByCustomerContact(contact)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        Page<Bill> page = billRepository.findByCustomer(customer, pageable);
        return page.map(billResponseMapper::toResponseDto);
    }

    /** Create a credit payment bill reducing existing customer credits. */
    public BillResponseDto createCreditBill(BillDto billDto) {
        if (billDto.getCustomer() == null || billDto.getCustomer().getCustomerId() == null) {
            throw new BillInformationInvalidException("Customer information is missing in the bill request");
        }
        Customer customer = customerRepository.findById(billDto.getCustomer().getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        double currentCredit = customer.getCustomerCredits();

        if(currentCredit < billDto.getBillTotalAmount() || billDto.getBillTotalAmount() < 0) {
            throw new InvalidCreditInformationException("Not a valid total for given bill of " + customer.getCustomerName());
        }
        customer.setCustomerCredits(currentCredit -billDto.getBillTotalAmount());
        customerRepository.save(customer);

        Bill newBill = new Bill();
        newBill.setBillType("creditsPayment");
        newBill.setBillStatus("complete");
        newBill.setCustomer(customer);
        newBill.setBillTotalAmount(billDto.getBillTotalAmount());
        newBill.setPaymentMethod(billDto.getPaymentMethod());

        Bill savedBill = billRepository.save(newBill);

        return billResponseMapper.toResponseDto(savedBill);

    }


    /** Delete bill by id returning boolean (true if deleted). */
    @Transactional
    public boolean deleteBillById(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new EntityNotFoundException("Bill not found with ID: " + billId));
        billRepository.delete(bill);
        return true;
    }

    public Page<BillResponseDto> searchBills(String rawQuery, Pageable pageable){
        String q = rawQuery == null ? "" : rawQuery.trim();
        if(q.isEmpty()){
            return getAllBills(pageable);
        }
        Page<Bill> page = billRepository.searchBills(q, pageable);
        return page.map(billResponseMapper::toResponseDto);
    }

}
