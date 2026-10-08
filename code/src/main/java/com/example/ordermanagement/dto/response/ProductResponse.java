package com.example.ordermanagement.dto.response;

import com.example.ordermanagement.enums.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String productCode;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
