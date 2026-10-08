package com.example.ordermanagement.util;

import com.example.ordermanagement.exception.BusinessValidationException;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public final class PageableValidator {

    private PageableValidator() {
    }

    public static void validateSort(Pageable pageable, Set<String> allowedProperties) {
        pageable.getSort().forEach(order -> {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new BusinessValidationException(
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + allowedProperties);
            }
        });
    }
}
