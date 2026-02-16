package com.orderms.order.kafka.event;

public class OrderCompletedEvent {
    private Long orderId;
    private String userId;
    private boolean success;
    private String message;

    // Constructors
    public OrderCompletedEvent() {}

    public OrderCompletedEvent(Long orderId, String userId, boolean success, String message) {
        this.orderId = orderId;
        this.userId = userId;
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}