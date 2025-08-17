package com.sarthak.BizNex.repository;

import com.sarthak.BizNex.entity.Bill;
import com.sarthak.BizNex.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

//    Optional<Bill> findByBillNumber(String billNumber);
    Optional<Bill> findByBillNumberIgnoreCase(String billNumber);

    List<Bill> findByCustomer(Customer customer);
    Page<Bill> findByCustomer(Customer customer, Pageable pageable);

    // Return / credit note lookup for original bill linkage
    List<Bill> findByOriginalBillNumberIgnoreCase(String originalBillNumber);

    @Query("SELECT b FROM Bill b JOIN b.customer c WHERE " +
            "LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.customerContact) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(b.billType) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(b.paymentMethod) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(b.originalBillNumber) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Bill> searchBills(@Param("q") String query, Pageable pageable);
    // Add custom query methods if needed
}
