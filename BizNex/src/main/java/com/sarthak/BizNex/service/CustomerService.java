package com.sarthak.BizNex.service;

import com.sarthak.BizNex.dto.CustomerDto;
import com.sarthak.BizNex.entity.Bill;
import com.sarthak.BizNex.entity.Customer;
import com.sarthak.BizNex.exception.DuplicateEntityException;
import com.sarthak.BizNex.exception.EntityNotFoundException;
import com.sarthak.BizNex.mapper.CustomerMapper;
import com.sarthak.BizNex.repository.BillRepository;
import com.sarthak.BizNex.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

/**
 * Customer domain service providing CRUD operations, contact-based lookups,
 * credit tracking, and paginated queries. Performs validation and duplicate
 * detection before delegating to persistence layer.
 */
@Service
public class CustomerService {
    // This service will handle customer-related operations
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final BillRepository billRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerService.class);

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper, BillRepository billRepository) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.billRepository = billRepository;
    }

    /** Return all customers as a non-paged list (utility / legacy) sorted alphabetically by name. */
    public List<CustomerDto> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll(Sort.by(Sort.Direction.ASC, "customerName"));
        return customerMapper.toDtoList(customers);
    }

    /** Paged retrieval of customers. */
    public Page<CustomerDto> getAllCustomers(Pageable pageable){
        Pageable effective = applyDefaultCustomerSort(pageable);
        return customerRepository.findAll(effective).map(customerMapper::toDto);
    }

    private Pageable applyDefaultCustomerSort(Pageable pageable) {
        if (pageable == null) return PageRequest.of(0,20, Sort.by("customerName").ascending());
        if (pageable.getSort().isUnsorted() || (pageable.getSort().stream().count()==1 && pageable.getSort().getOrderFor("customerId")!=null)) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("customerName").ascending());
        }
        return pageable;
    }

    /** Retrieve by numeric id or throw EntityNotFoundException. */
    public CustomerDto getCustomerById(Long id) {
        Optional<Customer> customer = customerRepository.findById(id);
        return customer.map(customerMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Customer with ID " + id + " not found."));
    }

    /** Delete by contact number returning deleted DTO. */
    @Transactional
    public CustomerDto deleteCustomerByContact(String contact) {
        Optional<Customer> customerOpt = customerRepository.findByCustomerContact(contact);
         if (customerOpt.isPresent()) {
             Customer customer = customerOpt.get();
             customerRepository.delete(customer);
             return customerMapper.toDto(customer);
         }
         else {
             throw new EntityNotFoundException("Customer with contact " + contact + " not found.");
         }
     }
    /** Create a new customer (enforces unique contact). */
    @Transactional
     public CustomerDto addCustomer(CustomerDto customerDto) {

        Optional<Customer> existingCustomer = customerRepository.findByCustomerContact(customerDto.getCustomerContact());
        if (existingCustomer.isPresent()) {
            throw new DuplicateEntityException("Customer with contact " + customerDto.getCustomerContact() + " already exists. Please use " +
                    "update to change details.");
        }else{
             Customer customer = customerMapper.toEntity(customerDto);
             customerRepository.save(customer);
             return customerMapper.toDto(customer);
        }
     }
    /** Partial style update; only non-null fields applied. */
    @Transactional
    public CustomerDto updateCustomer(CustomerDto customerDto) {
        Optional<Customer> existingCustomerOpt = customerRepository.findByCustomerContact(customerDto.getCustomerContact());
        if (existingCustomerOpt.isPresent()) {
            Customer existingCustomer = existingCustomerOpt.get();
            boolean isUpdated = false;

            Double oldCredits = existingCustomer.getCustomerCredits();
            Double requestedCredits = customerDto.getCustomerCredits();

            if (customerDto.getCustomerName() != null) { existingCustomer.setCustomerName(customerDto.getCustomerName()); isUpdated = true; }
            if (customerDto.getCustomerEmail() != null) { existingCustomer.setCustomerEmail(customerDto.getCustomerEmail()); isUpdated = true; }
            if (customerDto.getCustomerAddress() != null) { existingCustomer.setCustomerAddress(customerDto.getCustomerAddress()); isUpdated = true; }
            if (customerDto.getCustomerActiveStatus() != null) { existingCustomer.setCustomerActiveStatus(customerDto.getCustomerActiveStatus()); isUpdated = true; }

            // Handle credits update with automatic creditsPayment bill generation when credits decrease
            if (requestedCredits != null && !requestedCredits.equals(oldCredits)) {
                if (requestedCredits < oldCredits) {
                    double paymentAmount = oldCredits - requestedCredits;
                    // Create a creditsPayment bill representing the payment
                    Bill paymentBill = new Bill();
                    paymentBill.setCustomer(existingCustomer);
                    paymentBill.setBillType("creditsPayment");
                    paymentBill.setBillStatus("complete");
                    paymentBill.setPaymentMethod("cash"); // default; could be extended to accept via DTO
                    paymentBill.setBillTotalAmount(paymentAmount);
                    billRepository.save(paymentBill);
                    log.info("Created creditsPayment bill (amount={}) for customer contact={} due to credit reduction {} -> {}", paymentAmount, existingCustomer.getCustomerContact(), oldCredits, requestedCredits);
                } else {
                    log.info("Customer credits increased manually {} -> {} (no creditsPayment bill created)", oldCredits, requestedCredits);
                }
                existingCustomer.setCustomerCredits(requestedCredits);
                isUpdated = true;
            }

            if (customerDto.getCustomerContact() != null && !customerDto.getCustomerContact().equals(existingCustomer.getCustomerContact())) {
                if (customerRepository.existsByCustomerContact(customerDto.getCustomerContact())) {
                    throw new DuplicateEntityException("Customer with contact " + customerDto.getCustomerContact() + " already exists.");
                }
                existingCustomer.setCustomerContact(customerDto.getCustomerContact());
                isUpdated = true;
            }
            if (isUpdated) {
                customerRepository.save(existingCustomer);
            }
            return customerMapper.toDto(existingCustomer);
        } else {
            throw new EntityNotFoundException("Customer with ID " + customerDto.getCustomerId() + " not found.");
        }
    }

     /** Find by contact number or 404. */
     public CustomerDto getCustomerByContact(String contact) {
         Optional<Customer> customer = customerRepository.findByCustomerContact(contact);
         return customer.map(customerMapper::toDto).orElseThrow(()-> new EntityNotFoundException("Customer with contact " + contact + " not found."));
     }

     /** Non-paged list of customers with credits > 0 sorted alphabetically. */
     public List<CustomerDto> getCustomersWithCredits(){
        return customerRepository.findByCustomerCreditsGreaterThanOrderByCustomerNameAsc(0.0)
                .stream()
                .map(customerMapper::toDto)
                .toList();
     }

     /** Paged list of customers with credits > 0 (default alphabetical if default sort requested). */
     public Page<CustomerDto> getCustomersWithCredits(Pageable pageable){
        Pageable effective = applyDefaultCustomerSort(pageable);
        return customerRepository.findByCustomerCreditsGreaterThan(0.0, effective)
                .map(customerMapper::toDto);
    }

    public double totalPositiveCredits(){
        return customerRepository.sumPositiveCredits();
    }

    public double averagePositiveCredits(){
        return customerRepository.avgPositiveCredits();
    }

     /** Development bulk import – best-effort, duplicates skipped. */
    @Transactional
    public String addMultipleCustomers(List<CustomerDto> customerDtos) {
        StringBuilder response = new StringBuilder();
        for (CustomerDto customerDto : customerDtos) {
            try {
                CustomerDto addedCustomer = addCustomer(customerDto);
                response.append("Added Customer: ").append(addedCustomer.getCustomerName()).append("\n");
            } catch (DuplicateEntityException e) {
                response.append("Duplicate Customer: ").append(customerDto.getCustomerName()).append("\n");
            }
        }
        return response.toString();
    }

    public Page<CustomerDto> searchCustomers(String rawQuery, Pageable pageable){
        String q = rawQuery == null ? "" : rawQuery.trim();
        if(q.isEmpty()){
            return getAllCustomers(pageable); // preserve default sort logic
        }
        Pageable effective = applyDefaultCustomerSort(pageable);
        return customerRepository.searchCustomers(q, effective).map(customerMapper::toDto);
    }

    public Page<CustomerDto> searchCustomersWithCredits(String rawQuery, Pageable pageable){
        String q = rawQuery == null ? "" : rawQuery.trim();
        if(q.isEmpty()){
            return getCustomersWithCredits(pageable); // reuse existing logic
        }
        Pageable effective = applyDefaultCustomerSort(pageable);
        return customerRepository.searchCustomersWithCredits(q, effective).map(customerMapper::toDto);
    }
}
