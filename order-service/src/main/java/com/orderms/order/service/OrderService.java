package com.orderms.order.service;

import com.orderms.order.dto.OrderItemRequest;
import com.orderms.order.dto.OrderRequest;
import com.orderms.order.dto.OrderResponse;
import com.orderms.order.entity.Order;
import com.orderms.order.entity.OrderItem;
import com.orderms.order.entity.OrderStatus;
import com.orderms.order.exception.OrderNotFoundException;
import com.orderms.order.kafka.event.OrderCreatedEvent;
import com.orderms.order.kafka.producer.OrderEventProducer;
import com.orderms.order.repository.OrderRepository;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

//    @Transactional
//    @Retry(name = "database")
//    @TimeLimiter(name = "order-creation")
//    public CompletableFuture<OrderResponse> createOrder(OrderRequest request) {
//        log.info("Creating order for userId: {}", request.getUserId());
//
//        return CompletableFuture.supplyAsync(() -> {
//            // Calculate total amount
//            BigDecimal totalAmount = request.getItems().stream()
//                    .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            // Create order entity
//            Order order = new Order();
//            order.setUserId(request.getUserId());
//            order.setTotalAmount(totalAmount);
//            order.setStatus(OrderStatus.PENDING);
//
//            // Add order items
//            for (OrderItemRequest itemRequest : request.getItems()) {
//                OrderItem item = new OrderItem();
//                item.setProductId(itemRequest.getProductId());
//                item.setQuantity(itemRequest.getQuantity());
//                item.setPrice(itemRequest.getPrice());
//                order.addItem(item);
//            }
//
//            // Save order
//            Order savedOrder = orderRepository.save(order);
//            log.info("Order created successfully with id: {}", savedOrder.getId());
//
//            // Publish event to Kafka
//            OrderCreatedEvent event = OrderCreatedEvent.builder()
//                    .orderId(savedOrder.getId())
//                    .userId(savedOrder.getUserId())
//                    .totalAmount(savedOrder.getTotalAmount())
//                    .items(savedOrder.getItems().stream()
//                            .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
//                                    .productId(item.getProductId())
//                                    .quantity(item.getQuantity())
//                                    .price(item.getPrice())
//                                    .build())
//                            .collect(Collectors.toList()))
//                    .build();
//
//            orderEventProducer.publishOrderCreatedEvent(event);
//
//            return mapToOrderResponse(savedOrder);
//        });
//    }

    @Transactional
    @Retry(name = "database")
    public OrderResponse createOrder(OrderRequest request) {

        BigDecimal totalAmount = request.getItems().stream()
           .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
           .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        request.getItems().forEach(itemRequest -> {
            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(itemRequest.getPrice());
            order.addItem(item);
        });

        Order savedOrder = orderRepository.save(order);

            // Publish event to Kafka
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(savedOrder.getId())
                    .userId(savedOrder.getUserId())
                    .totalAmount(savedOrder.getTotalAmount())
                    .items(savedOrder.getItems().stream()
                            .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

        orderEventProducer.publishOrderCreatedEvent(event); // async Kafka ✅

        return mapToOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        log.info("Fetching order with id: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(String userId) {
        log.info("Fetching orders for userId: {}", userId);

        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");

        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark order as completed (called when saga completes successfully)
     */
    @Transactional
    public void completeOrder(Long orderId) {
        log.info("Completing order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);

        log.info("Order {} marked as COMPLETED", orderId);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        log.info("Updating order status for orderId: {} to {}", orderId, status);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        order.setStatus(status);
        orderRepository.save(order);

        log.info("Order status updated successfully for orderId: {}", orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        log.info("Cancelling order: {} - Reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order cancelled successfully: {}", orderId);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(order.getItems())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}