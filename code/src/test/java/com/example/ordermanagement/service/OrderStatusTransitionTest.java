package com.example.ordermanagement.service;

import com.example.ordermanagement.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTransitionTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
        "CREATED,    CONFIRMED,  true",
        "CREATED,    CANCELLED,  true",
        "CREATED,    PROCESSING, false",
        "CREATED,    SHIPPED,    false",
        "CREATED,    DELIVERED,  false",
        "CONFIRMED,  PROCESSING, true",
        "CONFIRMED,  CANCELLED,  true",
        "CONFIRMED,  CREATED,    false",
        "PROCESSING, SHIPPED,    true",
        "PROCESSING, CANCELLED,  true",
        "PROCESSING, CONFIRMED,  false",
        "SHIPPED,    DELIVERED,  true",
        "SHIPPED,    CANCELLED,  false",
        "DELIVERED,  CANCELLED,  false",
        "CANCELLED,  CREATED,    false",
        "CANCELLED,  CONFIRMED,  false"
    })
    void orderStatusTransitions(OrderStatus from, OrderStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    void createdAndConfirmedAndProcessing_areCancellable() {
        assertThat(OrderStatus.CREATED.isCancellable()).isTrue();
        assertThat(OrderStatus.CONFIRMED.isCancellable()).isTrue();
        assertThat(OrderStatus.PROCESSING.isCancellable()).isTrue();
    }

    @Test
    void shippedDeliveredCancelled_areNotCancellable() {
        assertThat(OrderStatus.SHIPPED.isCancellable()).isFalse();
        assertThat(OrderStatus.DELIVERED.isCancellable()).isFalse();
        assertThat(OrderStatus.CANCELLED.isCancellable()).isFalse();
    }
}
