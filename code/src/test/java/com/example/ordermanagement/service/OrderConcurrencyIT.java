package com.example.ordermanagement.service;

import com.example.ordermanagement.BaseIntegrationTest;
import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.dto.request.InventoryInitRequest;
import com.example.ordermanagement.dto.request.OrderItemRequest;
import com.example.ordermanagement.dto.request.OrderRequest;
import com.example.ordermanagement.dto.request.ProductRequest;
import com.example.ordermanagement.dto.response.InventoryResponse;
import com.example.ordermanagement.exception.InsufficientInventoryException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OrderConcurrencyIT extends BaseIntegrationTest {

    private static final int STOCK = 5;
    private static final int CONCURRENT_ORDERS = 12;

    @Autowired private CustomerService customerService;
    @Autowired private ProductService productService;
    @Autowired private InventoryService inventoryService;
    @Autowired private OrderService orderService;

    @Test
    void concurrentOrders_neverOversellInventory() throws Exception {
        long customerId = createCustomer("concurrency@example.com");
        long productId = createProductWithStock("Concurrency Product", STOCK);

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_ORDERS);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_ORDERS; i++) {
            results.add(pool.submit(() -> tryOrder(customerId, List.of(item(productId, 1)))));
        }
        long created = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                created++;
            }
        }
        pool.shutdown();

        InventoryResponse inventory = inventoryService.getInventory(productId);
        assertThat(created).isEqualTo(STOCK);
        assertThat(inventory.getAvailableQuantity()).isZero();
        assertThat(inventory.getReservedQuantity()).isEqualTo(STOCK);
    }

    @Test
    void failedOrder_rollsBackReservationsOfEarlierItems() {
        long customerId = createCustomer("rollback@example.com");
        long plentiful = createProductWithStock("Plentiful Product", 10);
        long scarce = createProductWithStock("Scarce Product", 1);

        OrderRequest request = orderRequest(customerId, List.of(item(plentiful, 4), item(scarce, 2)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientInventoryException.class);
        InventoryResponse inventory = inventoryService.getInventory(plentiful);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(10);
        assertThat(inventory.getReservedQuantity()).isZero();
    }

    private boolean tryOrder(long customerId, List<OrderItemRequest> items) {
        try {
            orderService.createOrder(orderRequest(customerId, items));
            return true;
        } catch (InsufficientInventoryException ex) {
            return false;
        }
    }

    private OrderRequest orderRequest(long customerId, List<OrderItemRequest> items) {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(customerId);
        request.setItems(items);
        return request;
    }

    private OrderItemRequest item(long productId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private long createCustomer(String email) {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Con");
        request.setLastName("Current");
        request.setEmail(email);
        return customerService.createCustomer(request).getId();
    }

    private long createProductWithStock(String name, int stock) {
        ProductRequest product = new ProductRequest();
        product.setName(name);
        product.setCategory("Test");
        product.setPrice(new BigDecimal("10.00"));
        long productId = productService.createProduct(product).getId();

        InventoryInitRequest inventory = new InventoryInitRequest();
        inventory.setAvailableQuantity(stock);
        inventory.setReorderLevel(0);
        inventoryService.initializeInventory(productId, inventory);
        return productId;
    }
}
