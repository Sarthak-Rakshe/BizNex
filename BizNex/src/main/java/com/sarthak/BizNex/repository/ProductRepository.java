package com.sarthak.BizNex.repository;

import com.sarthak.BizNex.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Active-only variants for soft-delete support
    List<Product> findByProductCategoryAndProductActiveTrue(String category);

    List<Product> findByProductNameContainingIgnoreCaseAndProductActiveTrue(String productName);

    Page<Product> findByProductActiveTrue(Pageable pageable);

    Page<Product> findByProductCategoryAndProductActiveTrue(String category, Pageable pageable);

    Page<Product> findByProductNameContainingIgnoreCaseAndProductActiveTrue(String productName, Pageable pageable);

    // Legacy/all records (used sparingly)
    List<Product> findByProductCategory(String category);

    boolean existsByProductCode(String productCode);

    Optional<Product> findByProductCode(String productCode);

    Optional<Product> findByProductNameAndProductCategory(String productName, String productCategory);

    List<Product> findByProductNameContainingIgnoreCase(String productName);

    Page<Product> findByProductCategory(String category, Pageable pageable);

    Page<Product> findByProductNameContainingIgnoreCase(String productName, Pageable pageable);

    // Default low-stock-first ordering (quantity < 10 first), then name asc, then id for stability
    @Query(value = "SELECT p FROM Product p WHERE p.productActive = true ORDER BY CASE WHEN p.productQuantity < 10 THEN 0 ELSE 1 END, LOWER(p.productName), p.productId",
           countQuery = "SELECT count(p) FROM Product p WHERE p.productActive = true")
    Page<Product> findAllOrderedLowStockFirst(Pageable pageable);

    @Query(value = "SELECT p FROM Product p WHERE p.productActive = true AND p.productCategory = :category ORDER BY CASE WHEN p.productQuantity < 10 THEN 0 ELSE 1 END, LOWER(p.productName), p.productId",
           countQuery = "SELECT count(p) FROM Product p WHERE p.productActive = true AND p.productCategory = :category")
    Page<Product> findByProductCategoryOrdered(@Param("category") String category, Pageable pageable);

    @Query(value = "SELECT p FROM Product p WHERE p.productActive = true AND LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%')) ORDER BY CASE WHEN p.productQuantity < 10 THEN 0 ELSE 1 END, LOWER(p.productName), p.productId",
           countQuery = "SELECT count(p) FROM Product p WHERE p.productActive = true AND LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%'))")
    Page<Product> searchByNameOrdered(@Param("productName") String productName, Pageable pageable);
}
