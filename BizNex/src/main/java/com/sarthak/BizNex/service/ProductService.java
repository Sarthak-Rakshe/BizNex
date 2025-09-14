package com.sarthak.BizNex.service;

import com.sarthak.BizNex.dto.ProductDto;
import com.sarthak.BizNex.entity.Product;
import com.sarthak.BizNex.exception.DuplicateEntityException;
import com.sarthak.BizNex.exception.EntityNotFoundException;
import com.sarthak.BizNex.mapper.ProductMapper;
import com.sarthak.BizNex.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Product domain service handling CRUD, search, category queries and code generation.
 * All persistence performed via ProductRepository; DTO mapping handled by ProductMapper.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper){
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    /**
     * Generate a unique productCode based on category/name prefixes and a numeric sequence.
     * Not concurrency-safe for extreme contention (DB unique constraint still enforces).
     */
    private String generateProductCode(String productCategory, String productName) {
        // Prefix: first 2 letters of category (uppercase)
        String prefix = productCategory != null && productCategory.length() >= 2
                ? productCategory.substring(0, 2).toUpperCase()
                : "PR";

        // Name: first 3 letters of product name (uppercase)
        String namePart = productName != null && productName.length() >= 3
                ? productName.substring(0, 3).toUpperCase()
                : (productName != null ? productName.toUpperCase() : "XXX");

        // Find the latest sequence for this prefix-name combination
        int sequence = 1;
        String productCode;
        do {
            productCode = prefix + namePart + String.format("%03d", sequence);
            sequence++;
        } while (productRepository.existsByProductCode(productCode));

        return productCode; // e.g., ELTAB001
    }

    /** Add a new product (auto-generates code if blank). */
    @Transactional
    public ProductDto addProduct(ProductDto productDto){
//        System.out.println("PRODUCT RECEIVED HAS GIVEN DATA: " + productDto);
        // Check for existing product by name and category
        Optional<Product> existingProduct = productRepository.findByProductNameAndProductCategory(
            productDto.getProductName(), productDto.getProductCategory());

        if (existingProduct.isPresent()) {
            Product existing = existingProduct.get();
            if (existing.isProductActive()) {
                // Active duplicate — disallow
                throw new DuplicateEntityException("Product already exists. Please use update to change details.");
            } else {
                // Reactivate soft-deleted product: update provided fields and mark active
                if (productDto.getProductDescription() != null) existing.setProductDescription(productDto.getProductDescription());
                if (productDto.getPricePerItem() != null) existing.setPricePerItem(productDto.getPricePerItem());
                if (productDto.getProductQuantity() != null) existing.setProductQuantity(productDto.getProductQuantity());
                // Category and name are same as query parameters; keep as-is but allow explicit overrides if provided (defensive)
                if (productDto.getProductName() != null) existing.setProductName(productDto.getProductName());
                if (productDto.getProductCategory() != null) existing.setProductCategory(productDto.getProductCategory());
                // Preserve existing productCode; only set if explicitly provided and non-blank
                if (productDto.getProductCode() != null && !productDto.getProductCode().isBlank()) {
                    existing.setProductCode(productDto.getProductCode());
                }
                existing.setProductActive(true);
                productRepository.save(existing);
                return productMapper.toDto(existing);
            }
        } else {
            Product product = productMapper.toEntity(productDto);
            // If productCode is missing or blank, generate a unique one
            if (product.getProductCode() == null || product.getProductCode().isBlank()) {
                product.setProductCode(generateProductCode(product.getProductCategory(), product.getProductName()));
            }
            productRepository.save(product);
            return productMapper.toDto(product);
        }
    }

    /** Retrieve product by id or throw EntityNotFoundException. */
    public ProductDto getProductById(Long id){
        Optional<Product> product = productRepository.findById(id);
        return productMapper.toDto(product.orElseThrow(() -> new EntityNotFoundException("Product with ID " + id + " not found.")));
    }

    /** Paged retrieval of products with default low-stock-first ordering if default sort (productId,asc). */
    public Page<ProductDto> getAllProducts(Pageable pageable){
        if (isDefaultProductSort(pageable)) {
            Page<Product> page = productRepository.findAllOrderedLowStockFirst(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
            return page.map(productMapper::toDto);
        }
        Page<Product> page = productRepository.findByProductActiveTrue(pageable);
        return page.map(productMapper::toDto);
    }

    private boolean isDefaultProductSort(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) return true; // treat unsorted as default
        // If single sort by productId ascending treat as default
        return pageable.getSort().stream().count()==1 && pageable.getSort().getOrderFor("productId") != null;
    }

    /** Partial update (null fields ignored). */
    @Transactional
    public ProductDto partialUpdateProduct(Long id, ProductDto productDto){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product with ID " + id + " not found."));
        if (productDto.getProductName() != null) product.setProductName(productDto.getProductName());
        if (productDto.getProductDescription() != null) product.setProductDescription(productDto.getProductDescription());
        if (productDto.getPricePerItem() != null) product.setPricePerItem(productDto.getPricePerItem());
        if (productDto.getProductQuantity() != null) product.setProductQuantity(productDto.getProductQuantity());
        if (productDto.getProductCategory() != null) product.setProductCategory(productDto.getProductCategory());
        if (productDto.getProductCode() != null && !productDto.getProductCode().isBlank()) product.setProductCode(productDto.getProductCode());
        productRepository.save(product);
        return productMapper.toDto(product);
    }

    /** Full update currently delegates to partial update (future: enforce full field presence). */
    @Transactional
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        // For now treat same as partial; could enforce full field presence later
        return partialUpdateProduct(id, productDto);
    }

    /** Delete product returning deleted representation (soft-delete: mark inactive). */
    @Transactional
    public ProductDto deleteProduct(Long id){
        Optional<Product> existingProduct = productRepository.findById(id);
        if(existingProduct.isPresent()){
            Product p = existingProduct.get();
            p.setProductActive(false);
            productRepository.save(p);
            return productMapper.toDto(p);
        }else{
            throw new EntityNotFoundException("Product with ID " + id + " not found.");
        }
    }

    /** List products by category (always returns list, may be empty) sorted alphabetically by productName (case-insensitive). */
   public List<ProductDto> getProductByCategory(String category){
        try {
            List<Product> list = productRepository.findByProductCategoryAndProductActiveTrue(category);
            return productMapper.toDtoList(
                    list.stream()
                        .sorted(Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER))
                        .toList()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving products for category: " + category, e);
        }
    }

    /** Find by unique product code. */
    public ProductDto getProductByProductCode(String productCode) {
        Optional<Product> product = productRepository.findByProductCode(productCode);
        return product.map(productMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Product with code " + productCode + " not found."));
    }

    /** Name substring search (case-insensitive) returning alphabetically sorted results. */
    public List<ProductDto> searchProductsByName(String productName) {
        List<Product> products = productRepository.findByProductNameContainingIgnoreCaseAndProductActiveTrue(productName)
                .stream()
                .sorted(Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return productMapper.toDtoList(products);
    }



    public Page<ProductDto> getProductByCategory(String category, Pageable pageable){
        if (isDefaultProductSort(pageable)) {
            Page<Product> page = productRepository.findByProductCategoryOrdered(category, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
            return page.map(productMapper::toDto);
        }
        Page<Product> page = productRepository.findByProductCategoryAndProductActiveTrue(category, pageable);
        return page.map(productMapper::toDto);
    }

    public Page<ProductDto> searchProductsByName(String productName, Pageable pageable) {
        if (isDefaultProductSort(pageable)) {
            Page<Product> page = productRepository.searchByNameOrdered(productName, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
            return page.map(productMapper::toDto);
        }
        Page<Product> page = productRepository.findByProductNameContainingIgnoreCaseAndProductActiveTrue(productName, pageable);
        return page.map(productMapper::toDto);
    }
}
