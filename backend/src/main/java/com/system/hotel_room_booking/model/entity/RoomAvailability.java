package com.system.hotel_room_booking.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_availability", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "date"}),
    indexes = {
        @Index(name = "idx_room_availability_room_date", columnList = "room_id, date"),
        @Index(name = "idx_room_availability_date", columnList = "date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAvailability {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal dynamicPrice; // null means use base price
    
    @Column(length = 255)
    private String notes;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
