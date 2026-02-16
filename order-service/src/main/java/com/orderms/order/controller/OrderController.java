package com.orderms.order.controller;

import com.orderms.order.dto.OrderRequest;
import com.orderms.order.dto.OrderResponse;
import com.orderms.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

//    @PostMapping
//    public CompletableFuture<ResponseEntity<OrderResponse>> createOrder(
//            @Valid @RequestBody OrderRequest request) {
//        log.info("Received request to create order for userId: {}", request.getUserId());
//
//        return orderService.createOrder(request)
//                .thenApply(orderResponse -> {
//                    log.info("Order created successfully with id: {}", orderResponse.getId());
//                    return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
//                });
//    }
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received request to create order for userId: {}", request.getUserId());

        OrderResponse response = orderService.createOrder(request); // now blocking

        log.info("Order created successfully with id: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        log.info("Received request to get order with id: {}", orderId);

        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(@PathVariable String userId) {
        log.info("Received request to get orders for userId: {}", userId);

        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("Received request to get all orders");

        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
}