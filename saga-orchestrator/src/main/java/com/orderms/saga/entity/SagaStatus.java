package com.orderms.saga.entity;

public enum SagaStatus {
    STARTED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    INVENTORY_PENDING,
    INVENTORY_RESERVED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}