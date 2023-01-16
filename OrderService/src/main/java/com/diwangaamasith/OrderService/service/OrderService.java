package com.diwangaamasith.OrderService.service;

import com.diwangaamasith.OrderService.model.OrderRequest;
import com.diwangaamasith.OrderService.model.OrderResponse;


public interface OrderService {
    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId);
}
