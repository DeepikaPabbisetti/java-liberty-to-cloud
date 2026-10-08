package com.example.ordermanagement.mapper;

import com.example.ordermanagement.dto.response.InventoryResponse;
import com.example.ordermanagement.entity.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productCode(inventory.getProduct().getProductCode())
                .productName(inventory.getProduct().getName())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .reorderLevel(inventory.getReorderLevel())
                .belowReorderLevel(inventory.getAvailableQuantity() <= inventory.getReorderLevel())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
