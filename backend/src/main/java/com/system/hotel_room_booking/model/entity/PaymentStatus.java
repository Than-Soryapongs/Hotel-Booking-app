package com.system.hotel_room_booking.model.entity;

/**
 * Enum for payment status tracking
 */
public enum PaymentStatus {
    PENDING,        // Payment initiated, awaiting customer action
    PROCESSING,     // Payment being processed by PayWay
    COMPLETED,      // Payment successful
    FAILED,         // Payment failed
    CANCELLED,      // Payment cancelled by user
    EXPIRED,        // Payment link expired
    REFUNDED        // Payment refunded
}
