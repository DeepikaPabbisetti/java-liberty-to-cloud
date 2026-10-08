package com.example.ordermanagement.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class NumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    @PersistenceContext
    private EntityManager entityManager;

    public String generateCustomerNumber() {
        return "CUST-" + nextValue("customer_number_seq");
    }

    public String generateProductCode() {
        return "PROD-" + nextValue("product_code_seq");
    }

    public String generateOrderNumber() {
        return "ORD-" + LocalDate.now().format(DATE_FORMAT) + "-"
                + String.format("%06d", nextValue("order_number_seq"));
    }

    private long nextValue(String sequenceName) {
        Number value = (Number) entityManager
                .createNativeQuery("SELECT nextval('" + sequenceName + "')")
                .getSingleResult();
        return value.longValue();
    }
}
