package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.OrderItemRequest;
import com.example.ordermanagement.dto.request.OrderRequest;
import com.example.ordermanagement.dto.response.OrderResponse;
import com.example.ordermanagement.dto.response.PageResponse;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.entity.OrderItem;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.enums.OrderStatus;
import com.example.ordermanagement.exception.InvalidOrderStateException;
import com.example.ordermanagement.exception.OrderNotFoundException;
import com.example.ordermanagement.mapper.OrderMapper;
import com.example.ordermanagement.repository.OrderRepository;
import com.example.ordermanagement.repository.OrderSpecifications;
import com.example.ordermanagement.util.NumberGenerator;
import com.example.ordermanagement.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("orderNumber", "orderDate", "status", "totalAmount", "createdAt", "updatedAt");

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;
    private final NumberGenerator numberGenerator;

    /**
     * Transaction boundary for order creation: the order, its items and the inventory
     * reservations are committed together or rolled back together (any exception
     * thrown here rolls the whole transaction back).
     * Items are processed in product-id order so concurrent orders acquire inventory
     * row locks in the same order and cannot deadlock.
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Customer customer = customerService.findById(request.getCustomerId());
        customerService.validateCustomerIsActive(customer);

        Order order = new Order();
        order.setOrderNumber(numberGenerator.generateOrderNumber());
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);

        BigDecimal orderTotal = BigDecimal.ZERO;
        List<OrderItemRequest> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequest::getProductId))
                .toList();

        for (OrderItemRequest itemRequest : sortedItems) {
            Product product = productService.findById(itemRequest.getProductId());
            productService.validateProductIsActive(product);

            inventoryService.reserveForOrder(product, itemRequest.getQuantity());

            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setLineTotal(lineTotal);

            order.addItem(item);
            orderTotal = orderTotal.add(lineTotal);
        }

        order.setTotalAmount(orderTotal);
        Order saved = orderRepository.save(order);
        log.info("Order created: {} for customer {} - total: {}",
                saved.getOrderNumber(), customer.getCustomerNumber(), saved.getTotalAmount());

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return orderMapper.toResponse(findByIdWithItems(id));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(Long customerId, OrderStatus status,
                                                 LocalDateTime from, LocalDateTime to,
                                                 Pageable pageable) {
        PageableValidator.validateSort(pageable, SORTABLE_FIELDS);
        Page<Order> page = orderRepository.findAll(OrderSpecifications.withFilters(customerId, status, from, to), pageable);
        return PageResponse.from(page, orderMapper::toResponse);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = findByIdWithItems(id);

        if (!order.getStatus().isCancellable()) {
            throw new InvalidOrderStateException(order.getStatus(), OrderStatus.CANCELLED);
        }

        order.getItems().forEach(item ->
                inventoryService.releaseForOrder(item.getProduct(), item.getQuantity()));

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        log.info("Order cancelled: {}", saved.getOrderNumber());
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = findByIdWithItems(id);
        OrderStatus current = order.getStatus();

        if (newStatus == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Use the cancel endpoint to cancel an order");
        }
        if (!current.canTransitionTo(newStatus)) {
            throw new InvalidOrderStateException(current, newStatus);
        }

        if (newStatus == OrderStatus.SHIPPED) {
            order.getItems().forEach(item ->
                    inventoryService.consumeReservation(item.getProduct(), item.getQuantity()));
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("Order {} status updated: {} -> {}", saved.getOrderNumber(), current, newStatus);
        return orderMapper.toResponse(saved);
    }

    private Order findByIdWithItems(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

}
