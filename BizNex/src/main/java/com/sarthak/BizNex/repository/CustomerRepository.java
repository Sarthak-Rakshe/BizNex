package com.sarthak.BizNex.repository;

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
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCustomerContact(String contact);

    void deleteByCustomerContact(String contact);

    Optional<Customer> findByCustomerContact(String contact);

    Page<Customer> findByCustomerCreditsGreaterThan(double minCredits, Pageable pageable);

    // New: non-paged, sorted list of customers with credits > min, alphabetically by name
    List<Customer> findByCustomerCreditsGreaterThanOrderByCustomerNameAsc(double minCredits);

    // Search across name, contact, email (case-insensitive contains)
    @Query("SELECT DISTINCT c FROM Customer c WHERE " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.customerContact) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.CustomerEmail) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Customer> searchCustomers(@Param("q") String query, Pageable pageable);

    // Search with credits > 0 filter
    @Query("SELECT DISTINCT c FROM Customer c WHERE c.customerCredits > 0 AND (" +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.customerContact) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.CustomerEmail) LIKE LOWER(CONCAT('%', :q, '%')) )")
    Page<Customer> searchCustomersWithCredits(@Param("q") String query, Pageable pageable);

    // Aggregations for customers with credits > 0
    @Query("SELECT COALESCE(SUM(c.customerCredits),0) FROM Customer c WHERE c.customerCredits > 0")
    double sumPositiveCredits();

    @Query("SELECT COALESCE(AVG(c.customerCredits),0) FROM Customer c WHERE c.customerCredits > 0")
    double avgPositiveCredits();
}
