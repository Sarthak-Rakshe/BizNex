package com.sarthak.BizNex.controller;

import com.sarthak.BizNex.dto.ProductDto;
import com.sarthak.BizNex.dto.response.PageResponseDto;
import com.sarthak.BizNex.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for managing products (CRUD, search, category filter) with pagination
 * and partial update (PATCH). Business logic lives in ProductService.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product management with default low-stock-first ordering on paged endpoints when using default sort")
public class ProductController {

    ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    /** Retrieve a single product by id (404 via exception if not found). */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable @Valid Long id){
        ProductDto productDto = productService.getProductById(id);
        return ResponseEntity.ok(productDto); // service throws if not found
    }

    /** Paged list of products with dynamic sort: field,direction (e.g. productName,desc). */
    @GetMapping()
    @Operation(summary = "List products (paged)", description = "Default ordering (when sort param is omitted or productId,asc) prioritizes low-stock items (quantity < 10) first, then productName ASC (case-insensitive), then productId for stability. Provide sort=field,dir to override.")
    public ResponseEntity<PageResponseDto<ProductDto>> getAllProducts(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size,
                                                                      @RequestParam(defaultValue = "productId,asc") String sort){
        Pageable pageable = buildPageable(page, size, sort);
        Page<ProductDto> dtoPage = productService.getAllProducts(pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (size <=0) size = 20;
        if (page <0) page = 0;
        String[] parts = sort.split(",");
        String sortField = parts.length > 0 ? parts[0] : "productId";
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }

    /** Create a new product – productCode auto-generated if blank. */
    @PostMapping()
    public ResponseEntity<ProductDto> addProduct(@RequestBody @Valid ProductDto productDto){
        ProductDto createdProduct = productService.addProduct(productDto);
        return ResponseEntity.ok(createdProduct);
    }

    /** Full (currently treated as partial) update of a product. */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto){
        ProductDto updatedProduct = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updatedProduct);
    }

    /** Partial update (PATCH semantics) – only non-null fields are applied. */
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> patchProduct(@PathVariable Long id, @RequestBody ProductDto productDto){
        ProductDto updated = productService.partialUpdateProduct(id, productDto);
        return ResponseEntity.ok(updated);
    }

    /** Delete product and return the deleted representation. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDto> deleteProduct(@PathVariable Long id){
        ProductDto deletedProduct = productService.deleteProduct(id);
        return ResponseEntity.ok(deletedProduct);
    }


    /** Paged category listing */
    @GetMapping("/category/{category}")
    @Operation(summary = "List products by category (paged)", description = "Uses low-stock-first default ordering unless a custom sort is supplied.")
    public ResponseEntity<PageResponseDto<ProductDto>> getProductByCategoryPaged(@PathVariable String category,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size,
                                                                                 @RequestParam(defaultValue = "productId,asc") String sort){
        Pageable pageable = buildPageable(page,size,sort);
        Page<ProductDto> dtoPage = productService.getProductByCategory(category, pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }


    /** Paged name search */
    @GetMapping("/search")
    @Operation(summary = "Search products by name (paged)", description = "Low-stock-first default ordering applies unless custom sort provided.")
    public ResponseEntity<PageResponseDto<ProductDto>> searchProductsByNamePaged(@RequestParam String productName,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size,
                                                                                 @RequestParam(defaultValue = "productId,asc") String sort){
        Pageable pageable = buildPageable(page,size,sort);
        Page<ProductDto> dtoPage = productService.searchProductsByName(productName, pageable);
        return ResponseEntity.ok(PageResponseDto.from(dtoPage));
    }

    /** Development-only bulk add helper (no transactional all-or-nothing). */
    @PostMapping("/bulk")
    public ResponseEntity<String> addMultipleProducts(@RequestBody List<ProductDto> productDtos) {
        String response = productService.addMultipleProducts(productDtos);
        return ResponseEntity.ok(response);
    }

}
