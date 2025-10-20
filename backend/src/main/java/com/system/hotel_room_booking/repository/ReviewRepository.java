package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Optional<Review> findByBookingId(Long bookingId);
    
    List<Review> findByUserId(Long userId);
    
    List<Review> findByRoomId(Long roomId);
    
    List<Review> findByRoomIdAndIsPublishedTrue(Long roomId);
    
    @Query("SELECT r FROM Review r WHERE r.room.id = :roomId AND r.isPublished = true ORDER BY r.createdAt DESC")
    List<Review> findPublishedReviewsByRoomId(@Param("roomId") Long roomId);
    
    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.room.id = :roomId AND r.isPublished = true")
    Double getAverageRatingForRoom(@Param("roomId") Long roomId);
    
    @Query("SELECT r FROM Review r WHERE r.overallRating >= :minRating AND r.isPublished = true")
    List<Review> findByMinimumRating(@Param("minRating") Integer minRating);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.room.id = :roomId AND r.isPublished = true")
    Long countPublishedReviewsByRoomId(@Param("roomId") Long roomId);
}
