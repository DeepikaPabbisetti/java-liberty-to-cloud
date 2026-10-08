package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.InventoryInitRequest;
import com.example.ordermanagement.dto.request.InventoryStockRequest;
import com.example.ordermanagement.dto.response.InventoryResponse;
import com.example.ordermanagement.entity.Inventory;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.exception.BusinessValidationException;
import com.example.ordermanagement.exception.InsufficientInventoryException;
import com.example.ordermanagement.exception.InventoryNotFoundException;
import com.example.ordermanagement.mapper.InventoryMapper;
import com.example.ordermanagement.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductService productService;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public InventoryResponse initializeInventory(Long productId, InventoryInitRequest request) {
        Product product = productService.findById(productId);

        if (inventoryRepository.existsByProductId(productId)) {
            throw new BusinessValidationException(
                    "Inventory already exists for product: " + product.getProductCode());
        }

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setAvailableQuantity(request.getAvailableQuantity());
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(request.getReorderLevel());

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory initialized for product {}: available={}", product.getProductCode(), saved.getAvailableQuantity());
        return inventoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        return inventoryMapper.toResponse(findByProductId(productId));
    }

    @Transactional
    public InventoryResponse addStock(Long productId, InventoryStockRequest request) {
        Inventory inventory = findByProductId(productId);
        int previous = inventory.getAvailableQuantity();
        inventory.setAvailableQuantity(previous + request.getQuantity());

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Stock added for product id {}: {} -> {}", productId, previous, saved.getAvailableQuantity());
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public InventoryResponse reduceStock(Long productId, InventoryStockRequest request) {
        Inventory inventory = findByProductId(productId);
        int available = inventory.getAvailableQuantity();

        if (available < request.getQuantity()) {
            throw new InsufficientInventoryException(
                    inventory.getProduct().getProductCode(), request.getQuantity(), available);
        }

        inventory.setAvailableQuantity(available - request.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Stock reduced for product id {}: {} -> {}", productId, available, saved.getAvailableQuantity());
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public InventoryResponse reserveStock(Long productId, InventoryStockRequest request) {
        Inventory inventory = findByProductIdForUpdate(productId);
        int available = inventory.getAvailableQuantity();

        if (available < request.getQuantity()) {
            throw new InsufficientInventoryException(
                    inventory.getProduct().getProductCode(), request.getQuantity(), available);
        }

        inventory.setAvailableQuantity(available - request.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Stock reserved for product id {}: reserved={}", productId, saved.getReservedQuantity());
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public InventoryResponse releaseReservedStock(Long productId, InventoryStockRequest request) {
        Inventory inventory = findByProductIdForUpdate(productId);
        int reserved = inventory.getReservedQuantity();

        if (reserved < request.getQuantity()) {
            throw new BusinessValidationException(
                    "Cannot release " + request.getQuantity() + " units — only " + reserved + " are reserved for product id: " + productId);
        }

        inventory.setReservedQuantity(reserved - request.getQuantity());
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.getQuantity());

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Reserved stock released for product id {}: released={}", productId, request.getQuantity());
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public void reserveForOrder(Product product, int quantity) {
        Inventory inventory = findByProductIdForUpdate(product.getId());
        int available = inventory.getAvailableQuantity();

        if (available < quantity) {
            throw new InsufficientInventoryException(product.getProductCode(), quantity, available);
        }

        inventory.setAvailableQuantity(available - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);

        log.info("Inventory reserved: product={}, qty={}", product.getProductCode(), quantity);
    }

    @Transactional
    public void releaseForOrder(Product product, int quantity) {
        Inventory inventory = findByProductIdForUpdate(product.getId());
        int reserved = inventory.getReservedQuantity();
        requireReserved(product, reserved, quantity);

        inventory.setReservedQuantity(reserved - quantity);
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
        inventoryRepository.save(inventory);

        log.info("Inventory released: product={}, qty={}", product.getProductCode(), quantity);
    }

    @Transactional
    public void consumeReservation(Product product, int quantity) {
        Inventory inventory = findByProductIdForUpdate(product.getId());
        int reserved = inventory.getReservedQuantity();
        requireReserved(product, reserved, quantity);

        inventory.setReservedQuantity(reserved - quantity);
        inventoryRepository.save(inventory);

        log.info("Inventory shipped: product={}, qty={}", product.getProductCode(), quantity);
    }

    private void requireReserved(Product product, int reserved, int quantity) {
        if (reserved < quantity) {
            throw new IllegalStateException("Reserved quantity " + reserved + " is lower than order quantity "
                    + quantity + " for product " + product.getProductCode());
        }
    }

    private Inventory findByProductId(Long productId) {
        productService.findById(productId);
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    private Inventory findByProductIdForUpdate(Long productId) {
        return inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }
}
