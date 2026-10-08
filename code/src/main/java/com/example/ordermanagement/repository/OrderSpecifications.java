package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> withFilters(Long customerId, OrderStatus status,
                                                   LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (customerId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("customer").get("id"), customerId));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("orderDate"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("orderDate"), to));
            }
            return predicate;
        };
    }
}
