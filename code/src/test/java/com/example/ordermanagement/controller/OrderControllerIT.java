package com.example.ordermanagement.controller;

import com.example.ordermanagement.BaseIntegrationTest;
import com.example.ordermanagement.dto.request.*;
import com.example.ordermanagement.dto.response.CustomerResponse;
import com.example.ordermanagement.dto.response.InventoryResponse;
import com.example.ordermanagement.dto.response.OrderResponse;
import com.example.ordermanagement.dto.response.ProductResponse;
import com.example.ordermanagement.enums.OrderStatus;
import com.example.ordermanagement.enums.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OrderControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final AtomicInteger emailCounter = new AtomicInteger(0);

    private Long customerId;
    private Long activeProductId;
    private Long inactiveProductId;

    @BeforeEach
    void setUp() {
        int uniqueId = emailCounter.incrementAndGet();

        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Order");
        customerRequest.setLastName("Test");
        customerRequest.setEmail("ordertest" + uniqueId + "@example.com");
        customerRequest.setCountry("USA");

        CustomerResponse customer = restTemplate
                .postForEntity("/api/customers", customerRequest, CustomerResponse.class)
                .getBody();
        assertThat(customer).isNotNull();
        customerId = customer.getId();

        ProductRequest productRequest = new ProductRequest();
        productRequest.setName("Test Product " + uniqueId);
        productRequest.setCategory("Electronics");
        productRequest.setPrice(new BigDecimal("100.00"));

        ProductResponse product = restTemplate
                .postForEntity("/api/products", productRequest, ProductResponse.class)
                .getBody();
        assertThat(product).isNotNull();
        activeProductId = product.getId();

        InventoryInitRequest inventoryRequest = new InventoryInitRequest();
        inventoryRequest.setAvailableQuantity(100);
        inventoryRequest.setReorderLevel(10);

        restTemplate.postForEntity("/api/inventory/" + activeProductId, inventoryRequest, InventoryResponse.class);

        ProductRequest inactiveProductRequest = new ProductRequest();
        inactiveProductRequest.setName("Inactive Product " + uniqueId);
        inactiveProductRequest.setCategory("Electronics");
        inactiveProductRequest.setPrice(new BigDecimal("50.00"));

        ProductResponse inactiveProduct = restTemplate
                .postForEntity("/api/products", inactiveProductRequest, ProductResponse.class)
                .getBody();
        assertThat(inactiveProduct).isNotNull();
        inactiveProductId = inactiveProduct.getId();

        ProductStatusRequest inactiveStatus = new ProductStatusRequest();
        inactiveStatus.setStatus(ProductStatus.INACTIVE);
        restTemplate.exchange(
                "/api/products/" + inactiveProductId + "/status",
                HttpMethod.PATCH, new HttpEntity<>(inactiveStatus), ProductResponse.class);
    }

    @Test
    void createOrder_withValidRequest_returns201WithCorrectTotal() {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(activeProductId);
        item.setQuantity(3);
        request.setItems(List.of(item));

        ResponseEntity<OrderResponse> response =
                restTemplate.postForEntity("/api/orders", request, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getBody().getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(response.getBody().getItems()).hasSize(1);
        assertThat(response.getBody().getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(response.getBody().getOrderNumber()).startsWith("ORD-");
    }

    @Test
    void createOrder_forInactiveProduct_returns400() {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(inactiveProductId);
        item.setQuantity(1);
        request.setItems(List.of(item));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createOrder_withInsufficientInventory_returns422() {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(activeProductId);
        item.setQuantity(999);
        request.setItems(List.of(item));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("INSUFFICIENT_INVENTORY");
    }

    @Test
    void createOrder_withZeroQuantity_returns400() {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(activeProductId);
        item.setQuantity(0);
        request.setItems(List.of(item));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createOrder_withEmptyItems_returns400() {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);
        request.setItems(List.of());

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancelOrder_withCreatedOrder_returns200AndReleasesInventory() {
        OrderResponse order = createTestOrder(1);

        ResponseEntity<OrderResponse> cancelResponse = restTemplate.postForEntity(
                "/api/orders/" + order.getId() + "/cancel", null, OrderResponse.class);

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody()).isNotNull();
        assertThat(cancelResponse.getBody().getStatus()).isEqualTo(OrderStatus.CANCELLED);

        ResponseEntity<InventoryResponse> inventoryResponse =
                restTemplate.getForEntity("/api/inventory/" + activeProductId, InventoryResponse.class);
        assertThat(inventoryResponse.getBody()).isNotNull();
        assertThat(inventoryResponse.getBody().getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    void cancelOrder_twiceOnSameOrder_returns422() {
        OrderResponse order = createTestOrder(1);

        restTemplate.postForEntity("/api/orders/" + order.getId() + "/cancel", null, OrderResponse.class);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/orders/" + order.getId() + "/cancel", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateOrderStatus_validTransition_returns200() {
        OrderResponse order = createTestOrder(1);

        OrderStatusRequest statusRequest = new OrderStatusRequest();
        statusRequest.setStatus(OrderStatus.CONFIRMED);

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/api/orders/" + order.getId() + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusRequest),
                OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatus_invalidTransition_returns422() {
        OrderResponse order = createTestOrder(1);

        OrderStatusRequest statusRequest = new OrderStatusRequest();
        statusRequest.setStatus(OrderStatus.DELIVERED);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/orders/" + order.getId() + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusRequest),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("INVALID_ORDER_STATE");
    }

    @Test
    void getOrderById_withExistingOrder_returns200() {
        OrderResponse created = createTestOrder(2);

        ResponseEntity<OrderResponse> response =
                restTemplate.getForEntity("/api/orders/" + created.getId(), OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrderNumber()).isEqualTo(created.getOrderNumber());
    }

    @Test
    void getOrderById_withNonExistingId_returns404() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/orders/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void inventoryIsReducedAfterOrderCreation() {
        ResponseEntity<InventoryResponse> before =
                restTemplate.getForEntity("/api/inventory/" + activeProductId, InventoryResponse.class);
        assertThat(before.getBody()).isNotNull();
        int beforeQty = before.getBody().getAvailableQuantity();

        createTestOrder(5);

        ResponseEntity<InventoryResponse> after =
                restTemplate.getForEntity("/api/inventory/" + activeProductId, InventoryResponse.class);
        assertThat(after.getBody()).isNotNull();
        assertThat(after.getBody().getAvailableQuantity()).isEqualTo(beforeQty - 5);
        assertThat(after.getBody().getReservedQuantity()).isEqualTo(5);
    }

    private OrderResponse createTestOrder(int quantity) {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(activeProductId);
        item.setQuantity(quantity);
        request.setItems(List.of(item));

        ResponseEntity<OrderResponse> response =
                restTemplate.postForEntity("/api/orders", request, OrderResponse.class);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }
}
