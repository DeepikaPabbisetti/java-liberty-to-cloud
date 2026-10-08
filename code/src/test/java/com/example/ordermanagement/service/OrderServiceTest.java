package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.OrderItemRequest;
import com.example.ordermanagement.dto.request.OrderRequest;
import com.example.ordermanagement.dto.response.OrderItemResponse;
import com.example.ordermanagement.dto.response.OrderResponse;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.entity.OrderItem;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.enums.CustomerStatus;
import com.example.ordermanagement.enums.OrderStatus;
import com.example.ordermanagement.enums.ProductStatus;
import com.example.ordermanagement.exception.BusinessValidationException;
import com.example.ordermanagement.exception.InvalidOrderStateException;
import com.example.ordermanagement.exception.OrderNotFoundException;
import com.example.ordermanagement.mapper.OrderMapper;
import com.example.ordermanagement.repository.OrderRepository;
import com.example.ordermanagement.util.NumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CustomerService customerService;
    @Mock private ProductService productService;
    @Mock private InventoryService inventoryService;
    @Mock private OrderMapper orderMapper;
    @Mock private NumberGenerator numberGenerator;

    @InjectMocks
    private OrderService orderService;

    private Customer activeCustomer;
    private Product activeProduct1;
    private Product activeProduct2;
    private Order createdOrder;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        activeCustomer = new Customer();
        activeCustomer.setId(1L);
        activeCustomer.setCustomerNumber("CUST-1001");
        activeCustomer.setFirstName("James");
        activeCustomer.setLastName("Anderson");
        activeCustomer.setStatus(CustomerStatus.ACTIVE);
        activeCustomer.setCreatedAt(LocalDateTime.now());
        activeCustomer.setUpdatedAt(LocalDateTime.now());

        activeProduct1 = new Product();
        activeProduct1.setId(101L);
        activeProduct1.setProductCode("PROD-1001");
        activeProduct1.setName("Enterprise Laptop Pro");
        activeProduct1.setPrice(new BigDecimal("1499.99"));
        activeProduct1.setStatus(ProductStatus.ACTIVE);
        activeProduct1.setCreatedAt(LocalDateTime.now());
        activeProduct1.setUpdatedAt(LocalDateTime.now());

        activeProduct2 = new Product();
        activeProduct2.setId(102L);
        activeProduct2.setProductCode("PROD-1002");
        activeProduct2.setName("Wireless Business Keyboard");
        activeProduct2.setPrice(new BigDecimal("89.99"));
        activeProduct2.setStatus(ProductStatus.ACTIVE);
        activeProduct2.setCreatedAt(LocalDateTime.now());
        activeProduct2.setUpdatedAt(LocalDateTime.now());

        createdOrder = new Order();
        createdOrder.setId(1L);
        createdOrder.setOrderNumber("ORD-20241008-000001");
        createdOrder.setCustomer(activeCustomer);
        createdOrder.setStatus(OrderStatus.CREATED);
        createdOrder.setOrderDate(LocalDateTime.now());
        createdOrder.setTotalAmount(new BigDecimal("3089.97"));
        createdOrder.setCreatedAt(LocalDateTime.now());
        createdOrder.setUpdatedAt(LocalDateTime.now());

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProduct(activeProduct1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("1499.99"));
        item1.setLineTotal(new BigDecimal("2999.98"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProduct(activeProduct2);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("89.99"));
        item2.setLineTotal(new BigDecimal("89.99"));

        createdOrder.getItems().add(item1);
        createdOrder.getItems().add(item2);

        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-20241008-000001")
                .customerId(1L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("3089.97"))
                .items(List.of(
                        OrderItemResponse.builder().id(1L).quantity(2).unitPrice(new BigDecimal("1499.99")).build(),
                        OrderItemResponse.builder().id(2L).quantity(1).unitPrice(new BigDecimal("89.99")).build()
                ))
                .build();
    }

    @Test
    void createOrder_withValidRequest_createsOrderSuccessfully() {
        OrderRequest request = buildOrderRequest();

        when(customerService.findById(1L)).thenReturn(activeCustomer);
        doNothing().when(customerService).validateCustomerIsActive(activeCustomer);
        when(productService.findById(101L)).thenReturn(activeProduct1);
        when(productService.findById(102L)).thenReturn(activeProduct2);
        doNothing().when(productService).validateProductIsActive(activeProduct1);
        doNothing().when(productService).validateProductIsActive(activeProduct2);
        doNothing().when(inventoryService).reserveForOrder(any(), anyInt());
        when(numberGenerator.generateOrderNumber()).thenReturn("ORD-20241008-000001");
        when(orderRepository.save(any(Order.class))).thenReturn(createdOrder);
        when(orderMapper.toResponse(createdOrder)).thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.getOrderNumber()).isEqualTo("ORD-20241008-000001");
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("3089.97");
        verify(inventoryService, times(2)).reserveForOrder(any(), anyInt());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_forInactiveCustomer_throwsBusinessValidationException() {
        OrderRequest request = buildOrderRequest();

        when(customerService.findById(1L)).thenReturn(activeCustomer);
        doThrow(new BusinessValidationException("Customer 'CUST-1001' is not active"))
                .when(customerService).validateCustomerIsActive(activeCustomer);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("not active");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_forInactiveProduct_throwsBusinessValidationException() {
        OrderRequest request = buildOrderRequest();
        activeProduct1.setStatus(ProductStatus.INACTIVE);

        when(customerService.findById(1L)).thenReturn(activeCustomer);
        doNothing().when(customerService).validateCustomerIsActive(activeCustomer);
        when(productService.findById(101L)).thenReturn(activeProduct1);
        doThrow(new BusinessValidationException("Product 'PROD-1001' is not available for ordering"))
                .when(productService).validateProductIsActive(activeProduct1);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("not available");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_withCancellableOrder_cancelsAndReleasesInventory() {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(createdOrder));
        doNothing().when(inventoryService).releaseForOrder(any(), anyInt());
        when(orderRepository.save(any(Order.class))).thenReturn(createdOrder);

        OrderResponse cancelledResponse = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.CANCELLED)
                .build();
        when(orderMapper.toResponse(createdOrder)).thenReturn(cancelledResponse);

        OrderResponse result = orderService.cancelOrder(1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService, times(2)).releaseForOrder(any(), anyInt());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void cancelOrder_withDeliveredOrder_throwsInvalidOrderStateException() {
        createdOrder.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(createdOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("CANCELLED");

        verify(inventoryService, never()).releaseForOrder(any(), anyInt());
    }

    @Test
    void cancelOrder_withShippedOrder_throwsInvalidOrderStateException() {
        createdOrder.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(createdOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void cancelOrder_withAlreadyCancelledOrder_throwsInvalidOrderStateException() {
        createdOrder.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(createdOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void updateOrderStatus_withValidTransition_updatesSuccessfully() {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(createdOrder));

        Order confirmedOrder = new Order();
        confirmedOrder.setId(1L);
        confirmedOrder.setOrderNumber("ORD-20241008-000001");
        confirmedOrder.setCustomer(activeCustomer);
        confirmedOrder.setStatus(OrderStatus.CONFIRMED);
        confirmedOrder.setTotalAmount(new BigDecimal("3089.97"));
        confirmedOrder.setOrderDate(LocalDateTime.now());
        confirmedOrder.setCreatedAt(LocalDateTime.now());
        confirmedOrder.setUpdatedAt(LocalDateTime.now());

        OrderResponse confirmedResponse = OrderResponse.builder()
                .id(1L).status(OrderStatus.CONFIRMED).build();

        when(orderRepository.save(any(Order.class))).thenReturn(confirmedOrder);
        when(orderMapper.toResponse(confirmedOrder)).thenReturn(confirmedResponse);

        OrderResponse result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatus_withInvalidTransition_throwsInvalidOrderStateException() {
        createdOrder.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(createdOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CREATED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    void getOrderById_withNonExistingId_throwsOrderNotFoundException() {
        when(orderRepository.findByIdWithItems(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }

    private OrderRequest buildOrderRequest() {
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(101L);
        item1.setQuantity(2);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(102L);
        item2.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(item1, item2));
        return request;
    }
}
