package com.example.ordermanagement.dto.request;

import com.example.ordermanagement.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductStatusRequest {

    @NotNull(message = "Status is required")
    private ProductStatus status;
}
