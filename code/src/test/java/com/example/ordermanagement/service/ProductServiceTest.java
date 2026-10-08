package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.ProductRequest;
import com.example.ordermanagement.dto.response.ProductResponse;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.enums.ProductStatus;
import com.example.ordermanagement.exception.BusinessValidationException;
import com.example.ordermanagement.exception.ProductNotFoundException;
import com.example.ordermanagement.mapper.ProductMapper;
import com.example.ordermanagement.repository.ProductRepository;
import com.example.ordermanagement.util.NumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;
    @Mock private NumberGenerator numberGenerator;

    @InjectMocks
    private ProductService productService;

    private Product activeProduct;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        activeProduct = new Product();
        activeProduct.setId(1L);
        activeProduct.setProductCode("PROD-1001");
        activeProduct.setName("Enterprise Laptop Pro");
        activeProduct.setCategory("Electronics");
        activeProduct.setPrice(new BigDecimal("1499.99"));
        activeProduct.setStatus(ProductStatus.ACTIVE);
        activeProduct.setCreatedAt(LocalDateTime.now());
        activeProduct.setUpdatedAt(LocalDateTime.now());

        productResponse = ProductResponse.builder()
                .id(1L)
                .productCode("PROD-1001")
                .name("Enterprise Laptop Pro")
                .price(new BigDecimal("1499.99"))
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void createProduct_withValidRequest_returnsCreatedProduct() {
        ProductRequest request = new ProductRequest();
        request.setName("Enterprise Laptop Pro");
        request.setCategory("Electronics");
        request.setPrice(new BigDecimal("1499.99"));

        when(numberGenerator.generateProductCode()).thenReturn("PROD-1001");
        when(productRepository.save(any(Product.class))).thenReturn(activeProduct);
        when(productMapper.toResponse(activeProduct)).thenReturn(productResponse);

        ProductResponse result = productService.createProduct(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Enterprise Laptop Pro");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getProductById_withExistingId_returnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(productMapper.toResponse(activeProduct)).thenReturn(productResponse);

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.getProductCode()).isEqualTo("PROD-1001");
    }

    @Test
    void getProductById_withNonExistingId_throwsProductNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateProduct_withValidData_updatesSuccessfully() {
        ProductRequest request = new ProductRequest();
        request.setName("Updated Laptop");
        request.setCategory("Electronics");
        request.setPrice(new BigDecimal("1599.99"));

        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setProductCode("PROD-1001");
        updatedProduct.setName("Updated Laptop");
        updatedProduct.setPrice(new BigDecimal("1599.99"));
        updatedProduct.setStatus(ProductStatus.ACTIVE);
        updatedProduct.setCreatedAt(LocalDateTime.now());
        updatedProduct.setUpdatedAt(LocalDateTime.now());

        ProductResponse updatedResponse = ProductResponse.builder()
                .id(1L)
                .name("Updated Laptop")
                .price(new BigDecimal("1599.99"))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
        when(productMapper.toResponse(updatedProduct)).thenReturn(updatedResponse);

        ProductResponse result = productService.updateProduct(1L, request);

        assertThat(result.getName()).isEqualTo("Updated Laptop");
    }

    @Test
    void validateProductIsActive_withActiveProduct_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> productService.validateProductIsActive(activeProduct));
    }

    @Test
    void validateProductIsActive_withInactiveProduct_throwsBusinessValidationException() {
        activeProduct.setStatus(ProductStatus.INACTIVE);

        assertThatThrownBy(() -> productService.validateProductIsActive(activeProduct))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void validateProductIsActive_withDiscontinuedProduct_throwsBusinessValidationException() {
        activeProduct.setStatus(ProductStatus.DISCONTINUED);

        assertThatThrownBy(() -> productService.validateProductIsActive(activeProduct))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("DISCONTINUED");
    }

    @Test
    void updateProductStatus_toDiscontinued_updatesSuccessfully() {
        Product discontinuedProduct = new Product();
        discontinuedProduct.setId(1L);
        discontinuedProduct.setProductCode("PROD-1001");
        discontinuedProduct.setStatus(ProductStatus.DISCONTINUED);
        discontinuedProduct.setCreatedAt(LocalDateTime.now());
        discontinuedProduct.setUpdatedAt(LocalDateTime.now());

        ProductResponse discontinuedResponse = ProductResponse.builder()
                .id(1L)
                .status(ProductStatus.DISCONTINUED)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any(Product.class))).thenReturn(discontinuedProduct);
        when(productMapper.toResponse(discontinuedProduct)).thenReturn(discontinuedResponse);

        ProductResponse result = productService.updateProductStatus(1L, ProductStatus.DISCONTINUED);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
    }
}
