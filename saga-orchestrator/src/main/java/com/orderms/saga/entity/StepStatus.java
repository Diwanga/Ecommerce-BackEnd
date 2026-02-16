package com.orderms.saga.entity;

public enum StepStatus {
    PENDING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}