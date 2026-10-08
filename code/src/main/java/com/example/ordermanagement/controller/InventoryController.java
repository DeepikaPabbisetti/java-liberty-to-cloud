package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.request.InventoryInitRequest;
import com.example.ordermanagement.dto.request.InventoryStockRequest;
import com.example.ordermanagement.dto.response.InventoryResponse;
import com.example.ordermanagement.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/{productId}")
    @Operation(summary = "Initialize inventory for a product")
    public ResponseEntity<InventoryResponse> initializeInventory(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryInitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.initializeInventory(productId, request));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory for a product")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @PostMapping("/{productId}/stock")
    @Operation(summary = "Add stock to inventory")
    public ResponseEntity<InventoryResponse> addStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryStockRequest request) {
        return ResponseEntity.ok(inventoryService.addStock(productId, request));
    }

    @PostMapping("/{productId}/reduce")
    @Operation(summary = "Reduce available stock")
    public ResponseEntity<InventoryResponse> reduceStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryStockRequest request) {
        return ResponseEntity.ok(inventoryService.reduceStock(productId, request));
    }

    @PostMapping("/{productId}/reserve")
    @Operation(summary = "Reserve stock for an order")
    public ResponseEntity<InventoryResponse> reserveStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryStockRequest request) {
        return ResponseEntity.ok(inventoryService.reserveStock(productId, request));
    }

    @PostMapping("/{productId}/release")
    @Operation(summary = "Release previously reserved stock")
    public ResponseEntity<InventoryResponse> releaseStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryStockRequest request) {
        return ResponseEntity.ok(inventoryService.releaseReservedStock(productId, request));
    }
}
