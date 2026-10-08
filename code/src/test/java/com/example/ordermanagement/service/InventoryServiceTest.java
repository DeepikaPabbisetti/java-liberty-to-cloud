package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.InventoryInitRequest;
import com.example.ordermanagement.dto.request.InventoryStockRequest;
import com.example.ordermanagement.dto.response.InventoryResponse;
import com.example.ordermanagement.entity.Inventory;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.enums.ProductStatus;
import com.example.ordermanagement.exception.BusinessValidationException;
import com.example.ordermanagement.exception.InsufficientInventoryException;
import com.example.ordermanagement.exception.InventoryNotFoundException;
import com.example.ordermanagement.mapper.InventoryMapper;
import com.example.ordermanagement.repository.InventoryRepository;
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
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductService productService;
    @Mock private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setProductCode("PROD-1001");
        product.setName("Enterprise Laptop Pro");
        product.setPrice(new BigDecimal("1499.99"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProduct(product);
        inventory.setAvailableQuantity(50);
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(10);
        inventory.setUpdatedAt(LocalDateTime.now());
        inventory.setVersion(0L);
    }

    @Test
    void initializeInventory_withValidRequest_createsInventory() {
        InventoryInitRequest request = new InventoryInitRequest();
        request.setAvailableQuantity(50);
        request.setReorderLevel(10);

        InventoryResponse response = InventoryResponse.builder()
                .id(1L).productId(1L).availableQuantity(50).reorderLevel(10).build();

        when(productService.findById(1L)).thenReturn(product);
        when(inventoryRepository.existsByProductId(1L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toResponse(inventory)).thenReturn(response);

        InventoryResponse result = inventoryService.initializeInventory(1L, request);

        assertThat(result.getAvailableQuantity()).isEqualTo(50);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void initializeInventory_whenAlreadyExists_throwsBusinessValidationException() {
        InventoryInitRequest request = new InventoryInitRequest();
        request.setAvailableQuantity(50);
        request.setReorderLevel(10);

        when(productService.findById(1L)).thenReturn(product);
        when(inventoryRepository.existsByProductId(1L)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.initializeInventory(1L, request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void addStock_increasesAvailableQuantity() {
        InventoryStockRequest request = new InventoryStockRequest();
        request.setQuantity(20);

        Inventory updatedInventory = new Inventory();
        updatedInventory.setProduct(product);
        updatedInventory.setAvailableQuantity(70);
        updatedInventory.setReservedQuantity(0);
        updatedInventory.setReorderLevel(10);
        updatedInventory.setUpdatedAt(LocalDateTime.now());

        InventoryResponse response = InventoryResponse.builder()
                .availableQuantity(70).build();

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(productService.findById(1L)).thenReturn(product);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
        when(inventoryMapper.toResponse(updatedInventory)).thenReturn(response);

        InventoryResponse result = inventoryService.addStock(1L, request);

        assertThat(result.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    void reduceStock_withSufficientStock_reducesSuccessfully() {
        InventoryStockRequest request = new InventoryStockRequest();
        request.setQuantity(10);

        Inventory updatedInventory = new Inventory();
        updatedInventory.setProduct(product);
        updatedInventory.setAvailableQuantity(40);
        updatedInventory.setReservedQuantity(0);
        updatedInventory.setReorderLevel(10);
        updatedInventory.setUpdatedAt(LocalDateTime.now());

        InventoryResponse response = InventoryResponse.builder()
                .availableQuantity(40).build();

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(productService.findById(1L)).thenReturn(product);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
        when(inventoryMapper.toResponse(updatedInventory)).thenReturn(response);

        InventoryResponse result = inventoryService.reduceStock(1L, request);

        assertThat(result.getAvailableQuantity()).isEqualTo(40);
    }

    @Test
    void reduceStock_withInsufficientStock_throwsInsufficientInventoryException() {
        InventoryStockRequest request = new InventoryStockRequest();
        request.setQuantity(100);

        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(productService.findById(1L)).thenReturn(product);

        assertThatThrownBy(() -> inventoryService.reduceStock(1L, request))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("PROD-1001")
                .hasMessageContaining("100")
                .hasMessageContaining("50");
    }

    @Test
    void reserveForOrder_withSufficientStock_reservesSuccessfully() {
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));

        inventoryService.reserveForOrder(product, 5);

        verify(inventoryRepository).save(argThat(inv ->
                inv.getAvailableQuantity() == 45 && inv.getReservedQuantity() == 5));
    }

    @Test
    void reserveForOrder_withInsufficientStock_throwsInsufficientInventoryException() {
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserveForOrder(product, 100))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void releaseForOrder_restoresAvailableAndDecreasesReserved() {
        inventory.setAvailableQuantity(45);
        inventory.setReservedQuantity(5);

        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));

        inventoryService.releaseForOrder(product, 5);

        verify(inventoryRepository).save(argThat(inv ->
                inv.getAvailableQuantity() == 50 && inv.getReservedQuantity() == 0));
    }

    @Test
    void getInventory_withNonExistingProductId_throwsInventoryNotFoundException() {
        when(productService.findById(99L)).thenReturn(product);
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventory(99L))
                .isInstanceOf(InventoryNotFoundException.class);
    }
}
