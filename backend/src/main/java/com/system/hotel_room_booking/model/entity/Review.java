package com.system.hotel_room_booking.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_booking_id", columnList = "booking_id"),
    @Index(name = "idx_review_user_id", columnList = "user_id"),
    @Index(name = "idx_review_room_id", columnList = "room_id"),
    @Index(name = "idx_review_overall_rating", columnList = "overallRating")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    // Multi-dimensional ratings
    @Column(nullable = false)
    private Integer overallRating; // 1-5
    
    @Column(nullable = false)
    private Integer cleanlinessRating; // 1-5
    
    @Column(nullable = false)
    private Integer comfortRating; // 1-5
    
    @Column(nullable = false)
    private Integer serviceRating; // 1-5
    
    @Column(nullable = false)
    private Integer valueForMoneyRating; // 1-5
    
    @Column(nullable = false)
    private Integer locationRating; // 1-5
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = true; // Verified bookings only
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublished = true;
    
    @Column
    private LocalDateTime publishedAt;
    
    @Column(columnDefinition = "TEXT")
    private String managementResponse;
    
    @Column
    private LocalDateTime respondedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
