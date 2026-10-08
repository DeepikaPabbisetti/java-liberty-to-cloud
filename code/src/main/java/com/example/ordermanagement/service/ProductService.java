package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.ProductRequest;
import com.example.ordermanagement.dto.response.PageResponse;
import com.example.ordermanagement.dto.response.ProductResponse;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.enums.ProductStatus;
import com.example.ordermanagement.exception.BusinessValidationException;
import com.example.ordermanagement.exception.ProductNotFoundException;
import com.example.ordermanagement.mapper.ProductMapper;
import com.example.ordermanagement.repository.ProductRepository;
import com.example.ordermanagement.repository.ProductSpecifications;
import com.example.ordermanagement.util.NumberGenerator;
import com.example.ordermanagement.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("name", "productCode", "category", "price", "status", "createdAt", "updatedAt");

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final NumberGenerator numberGenerator;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setProductCode(numberGenerator.generateProductCode());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setStatus(ProductStatus.ACTIVE);

        Product saved = productRepository.save(product);
        log.info("Product created: {} - {}", saved.getProductCode(), saved.getName());
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductByCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(ProductStatus status, String category,
                                                     String search, Pageable pageable) {
        PageableValidator.validateSort(pageable, SORTABLE_FIELDS);
        Page<Product> page = productRepository.findAll(ProductSpecifications.withFilters(status, category, search), pageable);
        return PageResponse.from(page, productMapper::toResponse);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findById(id);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());

        Product saved = productRepository.save(product);
        log.info("Product updated: {}", saved.getProductCode());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatus status) {
        Product product = findById(id);
        ProductStatus previous = product.getStatus();
        product.setStatus(status);

        Product saved = productRepository.save(product);
        log.info("Product {} status changed: {} -> {}", saved.getProductCode(), previous, saved.getStatus());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
        log.info("Product deleted: {}", product.getProductCode());
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public void validateProductIsActive(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "Product '" + product.getProductCode() + "' is not available for ordering (status: " + product.getStatus() + ")");
        }
    }

}
