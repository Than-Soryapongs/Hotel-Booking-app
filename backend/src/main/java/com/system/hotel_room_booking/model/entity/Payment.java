package com.system.hotel_room_booking.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to track all payment transactions
 * Stores PayWay transaction details and status
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_transaction_id", columnList = "transactionId"),
    @Index(name = "idx_payment_cart_id", columnList = "cart_id"),
    @Index(name = "idx_payment_booking_id", columnList = "booking_id"),
    @Index(name = "idx_payment_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Transaction identifiers
    @Column(unique = true, nullable = false, length = 50)
    private String transactionId;     // Our internal transaction ID

    @Column(length = 50)
    private String paywayTransactionId; // PayWay's transaction reference (if different)

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // Payment details
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 50)
    private String paymentMethod;     // e.g., "ABA PAY", "CARD", "KHQR"

    // PayWay specific fields
    @Column(length = 20)
    private String reqTime;           // Request time sent to PayWay

    @Column(columnDefinition = "TEXT")
    private String paywayHash;        // Hash sent to PayWay

    @Column(columnDefinition = "TEXT")
    private String callbackHash;      // Hash received from PayWay callback

    @Column(columnDefinition = "TEXT")
    private String paymentUrl;        // PayWay payment URL

    // Callback data
    @Column
    private LocalDateTime callbackReceivedAt;

    @Column(columnDefinition = "TEXT")
    private String callbackData;      // Full callback payload for reference

    @Column(columnDefinition = "TEXT")
    private String errorMessage;      // Error message if payment failed

    // Customer information (stored for record-keeping)
    @Column(length = 100)
    private String customerName;

    @Column(length = 100)
    private String customerEmail;

    @Column(length = 20)
    private String customerPhone;

    // Timestamps
    @Column
    private LocalDateTime initiatedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime failedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
