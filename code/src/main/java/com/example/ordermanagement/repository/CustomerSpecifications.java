package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.enums.CustomerStatus;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> withFilters(CustomerStatus status, String search) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }
            return predicate;
        };
    }
}
