package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.request.ProductRequest;
import com.example.ordermanagement.dto.request.ProductStatusRequest;
import com.example.ordermanagement.dto.response.PageResponse;
import com.example.ordermanagement.dto.response.ProductResponse;
import com.example.ordermanagement.enums.ProductStatus;
import com.example.ordermanagement.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/code/{productCode}")
    @Operation(summary = "Get product by product code")
    public ResponseEntity<ProductResponse> getProductByCode(@PathVariable String productCode) {
        return ResponseEntity.ok(productService.getProductByCode(productCode));
    }

    @GetMapping
    @Operation(summary = "List products with optional filtering and pagination")
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getProducts(status, category, search, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product details")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update product status")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusRequest request) {
        return ResponseEntity.ok(productService.updateProductStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
