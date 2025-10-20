package com.system.hotel_room_booking.controller;

import com.system.hotel_room_booking.model.dto.request.CreateReviewRequest;
import com.system.hotel_room_booking.model.dto.response.ReviewResponse;
import com.system.hotel_room_booking.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review Management", description = "APIs for managing guest reviews and ratings")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "cookieAuth")
    @Operation(
        summary = "Create a review",
        description = "Create a review for a checked-out booking with multi-dimensional ratings"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Review created successfully",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid review data or booking not eligible"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        log.info("REST request to create review");
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get review by ID",
        description = "Retrieve detailed information about a specific review"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review found",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @ApiResponse(responseCode = "404", description = "Review not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReviewResponse> getReviewById(
            @Parameter(description = "Review ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to get review: {}", id);
        ReviewResponse response = reviewService.getReviewById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}")
    @Operation(
        summary = "Get reviews for a room",
        description = "Retrieve all published reviews for a specific room"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved reviews"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ReviewResponse>> getRoomReviews(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId) {
        log.info("REST request to get reviews for room: {}", roomId);
        List<ReviewResponse> response = reviewService.getRoomReviews(roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-reviews")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "cookieAuth")
    @Operation(
        summary = "Get current user's reviews",
        description = "Retrieve all reviews written by the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved reviews"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        log.info("REST request to get reviews for current user");
        List<ReviewResponse> response = reviewService.getUserReviews();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}/ratings")
    @Operation(
        summary = "Get room ratings summary",
        description = "Get average rating and review count for a room"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved ratings"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getRoomRatings(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId) {
        log.info("REST request to get ratings for room: {}", roomId);
        Map<String, Object> ratings = reviewService.getRoomRatings(roomId);
        return ResponseEntity.ok(ratings);
    }

    @PostMapping("/{id}/response")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Add management response",
        description = "Add a management response to a review. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Response added successfully",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @ApiResponse(responseCode = "404", description = "Review not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<ReviewResponse> addManagementResponse(
            @Parameter(description = "Review ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Management response text", required = true)
            @RequestParam String response) {
        log.info("REST request to add management response to review: {}", id);
        ReviewResponse reviewResponse = reviewService.addManagementResponse(id, response);
        return ResponseEntity.ok(reviewResponse);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Toggle review publish status",
        description = "Publish or unpublish a review. Admin access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status updated successfully",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @ApiResponse(responseCode = "404", description = "Review not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<ReviewResponse> togglePublishStatus(
            @Parameter(description = "Review ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Published status", required = true)
            @RequestParam boolean published) {
        log.info("REST request to toggle publish status for review: {} to {}", id, published);
        ReviewResponse response = reviewService.togglePublishStatus(id, published);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "cookieAuth")
    @Operation(
        summary = "Delete a review",
        description = "Delete a review. Users can only delete their own reviews."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Review not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete this review"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Review ID", required = true)
            @PathVariable Long id) {
        log.info("REST request to delete review: {}", id);
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
