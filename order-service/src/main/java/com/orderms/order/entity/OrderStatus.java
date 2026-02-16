package com.orderms.order.entity;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_COMPLETED,
    INVENTORY_RESERVED,
    COMPLETED,
    CANCELLED,
    FAILED
}