package com.system.hotel_room_booking.service;

import com.system.hotel_room_booking.exception.ResourceNotFoundException;
import com.system.hotel_room_booking.model.dto.request.CreateReviewRequest;
import com.system.hotel_room_booking.model.dto.response.ReviewResponse;
import com.system.hotel_room_booking.model.dto.response.RoomSummaryResponse;
import com.system.hotel_room_booking.model.dto.response.UserSummaryResponse;
import com.system.hotel_room_booking.model.entity.*;
import com.system.hotel_room_booking.repository.BookingRepository;
import com.system.hotel_room_booking.repository.ReviewRepository;
import com.system.hotel_room_booking.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Create a review for a booking
     */
    public ReviewResponse createReview(CreateReviewRequest request) {
        log.info("Creating review for booking: {}", request.getBookingId());
        User user = getCurrentUser();

        // Get booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        // Validate user ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only review your own bookings");
        }

        // Validate booking status (can only review checked-out bookings)
        if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("Can only review checked-out bookings");
        }

        // Check if review already exists
        if (booking.getReview() != null) {
            throw new IllegalArgumentException("Review already exists for this booking");
        }

        // Create review
        Review review = Review.builder()
                .booking(booking)
                .user(user)
                .room(booking.getRoom())
                .overallRating(request.getOverallRating())
                .cleanlinessRating(request.getCleanlinessRating())
                .comfortRating(request.getComfortRating())
                .serviceRating(request.getServiceRating())
                .valueForMoneyRating(request.getValueForMoneyRating())
                .locationRating(request.getLocationRating())
                .comment(request.getComment())
                .isVerified(true) // Verified because it's linked to a real booking
                .isPublished(true) // Auto-publish (can add moderation later)
                .publishedAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Review created successfully for booking: {}", request.getBookingId());

        return mapToReviewResponse(savedReview);
    }

    /**
     * Get review by ID
     */
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long reviewId) {
        log.info("Fetching review: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        return mapToReviewResponse(review);
    }

    /**
     * Get reviews for a room
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getRoomReviews(Long roomId) {
        log.info("Fetching reviews for room: {}", roomId);

        return reviewRepository.findPublishedReviewsByRoomId(roomId).stream()
                .map(this::mapToReviewResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get reviews by current user
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getUserReviews() {
        User user = getCurrentUser();
        log.info("Fetching reviews by user: {}", user.getId());

        return reviewRepository.findByUserId(user.getId()).stream()
                .map(this::mapToReviewResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get average ratings for a room
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoomRatings(Long roomId) {
        log.info("Calculating ratings for room: {}", roomId);

        Double avgOverallRating = reviewRepository.getAverageRatingForRoom(roomId);
        Long reviewCount = reviewRepository.countPublishedReviewsByRoomId(roomId);

        Map<String, Object> ratings = new HashMap<>();
        ratings.put("averageRating", avgOverallRating != null ? avgOverallRating : 0.0);
        ratings.put("reviewCount", reviewCount);

        return ratings;
    }

    /**
     * Add management response to a review
     */
    public ReviewResponse addManagementResponse(Long reviewId, String response) {
        log.info("Adding management response to review: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setManagementResponse(response);
        review.setRespondedAt(LocalDateTime.now());

        Review updatedReview = reviewRepository.save(review);
        log.info("Management response added to review: {}", reviewId);

        return mapToReviewResponse(updatedReview);
    }

    /**
     * Publish/unpublish a review
     */
    public ReviewResponse togglePublishStatus(Long reviewId, boolean published) {
        log.info("Toggling publish status for review: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setIsPublished(published);
        if (published && review.getPublishedAt() == null) {
            review.setPublishedAt(LocalDateTime.now());
        }

        Review updatedReview = reviewRepository.save(review);
        log.info("Review publish status updated: {}", reviewId);

        return mapToReviewResponse(updatedReview);
    }

    /**
     * Delete a review
     */
    public void deleteReview(Long reviewId) {
        log.info("Deleting review: {}", reviewId);
        User user = getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        // Validate ownership
        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        log.info("Review deleted successfully: {}", reviewId);
    }

    // Helper methods

    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .user(mapToUserSummary(review.getUser()))
                .room(mapToRoomSummary(review.getRoom()))
                .overallRating(review.getOverallRating())
                .cleanlinessRating(review.getCleanlinessRating())
                .comfortRating(review.getComfortRating())
                .serviceRating(review.getServiceRating())
                .valueForMoneyRating(review.getValueForMoneyRating())
                .locationRating(review.getLocationRating())
                .comment(review.getComment())
                .isVerified(review.getIsVerified())
                .isPublished(review.getIsPublished())
                .publishedAt(review.getPublishedAt())
                .managementResponse(review.getManagementResponse())
                .respondedAt(review.getRespondedAt())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private UserSummaryResponse mapToUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(null) // phoneNumber not in User entity
                .build();
    }

    private RoomSummaryResponse mapToRoomSummary(Room room) {
        return RoomSummaryResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .type(room.getType())
                .floor(room.getFloor())
                .basePrice(room.getBasePrice())
                .build();
    }
}
