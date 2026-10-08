package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.enums.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(ProductStatus status, String category, String search) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (category != null && !category.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("productCode")), pattern)));
            }
            return predicate;
        };
    }
}
