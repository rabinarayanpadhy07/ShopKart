package com.example.demo.entity;

public enum OrderStatus {
    PENDING,
    SUCCESS, // Confirmed payment
    FAILED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURN_APPROVED,
    RETURN_REJECTED,
    ITEM_PICKED_UP,
    RETURNED,
    REFUNDED
}
