package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.*;
import com.example.ordermanagement.enums.CustomerStatus;
import com.example.ordermanagement.enums.OrderStatus;
import com.example.ordermanagement.enums.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Customer customer;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerNumber("CUST-T001");
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        customer.setEmail("test.order@example.com");
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(customer);

        product = new Product();
        product.setProductCode("PROD-T001");
        product.setName("Test Product");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("99.99"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(product);

        order = new Order();
        order.setOrderNumber("ORD-TEST-000001");
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("99.99"));
        item.setLineTotal(new BigDecimal("199.98"));
        order.addItem(item);

        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByOrderNumber_withExistingOrder_returnsOrder() {
        Optional<Order> result = orderRepository.findByOrderNumber("ORD-TEST-000001");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void findByIdWithItems_loadsItemsAndProducts() {
        Optional<Order> result = orderRepository.findByIdWithItems(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getItems()).hasSize(1);
        assertThat(result.get().getItems().get(0).getProduct().getProductCode()).isEqualTo("PROD-T001");
    }

    @Test
    void filter_byCustomerId_returnsOrdersForCustomer() {
        Page<Order> result = orderRepository.findAll(
                OrderSpecifications.withFilters(customer.getId(), null, null, null), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-TEST-000001");
    }

    @Test
    void filter_byStatus_returnsOrdersWithMatchingStatus() {
        Page<Order> result = orderRepository.findAll(
                OrderSpecifications.withFilters(null, OrderStatus.CREATED, null, null), PageRequest.of(0, 10));

        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(o -> o.getStatus() == OrderStatus.CREATED);
    }

    @Test
    void filter_withCustomerIdAndStatus_returnsMatchingOrders() {
        Page<Order> result = orderRepository.findAll(
                OrderSpecifications.withFilters(customer.getId(), OrderStatus.CREATED, null, null), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void filter_withNonMatchingStatus_returnsEmpty() {
        Page<Order> result = orderRepository.findAll(
                OrderSpecifications.withFilters(customer.getId(), OrderStatus.DELIVERED, null, null), PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

}
