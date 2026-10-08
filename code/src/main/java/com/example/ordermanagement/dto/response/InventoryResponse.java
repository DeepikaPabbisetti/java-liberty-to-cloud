package com.example.ordermanagement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryResponse {

    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer reorderLevel;
    private boolean belowReorderLevel;
    private LocalDateTime updatedAt;
}
