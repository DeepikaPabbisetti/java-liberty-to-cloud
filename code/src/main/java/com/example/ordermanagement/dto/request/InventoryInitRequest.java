package com.example.ordermanagement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventoryInitRequest {

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity must be zero or greater")
    private Integer availableQuantity;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level must be zero or greater")
    private Integer reorderLevel;
}
