package com.system.hotel_room_booking.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_discounts", indexes = {
    @Index(name = "idx_booking_discount_booking_id", columnList = "booking_id"),
    @Index(name = "idx_booking_discount_discount_id", columnList = "discount_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDiscount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(length = 50)
    private String discountCode; // The actual code used
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;
}
