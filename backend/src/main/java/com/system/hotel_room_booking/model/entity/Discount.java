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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "discounts", indexes = {
    @Index(name = "idx_discount_code", columnList = "code"),
    @Index(name = "idx_discount_type", columnList = "type"),
    @Index(name = "idx_discount_valid_dates", columnList = "validFrom, validUntil")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String code;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscountType type;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal percentageValue; // For percentage discounts
    
    @Column(precision = 10, scale = 2)
    private BigDecimal fixedAmount; // For fixed amount discounts
    
    @Column
    private LocalDateTime validFrom;
    
    @Column
    private LocalDateTime validUntil;
    
    @Column
    private Integer maxUsageCount; // null = unlimited
    
    @Column(nullable = false)
    @Builder.Default
    private Integer currentUsageCount = 0;
    
    @Column
    private Integer maxUsagePerUser; // null = unlimited
    
    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderAmount; // Minimum booking amount required
    
    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount; // Maximum discount cap
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(columnDefinition = "TEXT")
    private String termsAndConditions;
    
    @OneToMany(mappedBy = "discount")
    @Builder.Default
    private Set<BookingDiscount> bookingDiscounts = new HashSet<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
