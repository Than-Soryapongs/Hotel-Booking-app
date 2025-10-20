package com.system.hotel_room_booking.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Overall rating is required")
    @Min(value = 1, message = "Overall rating must be between 1 and 5")
    @Max(value = 5, message = "Overall rating must be between 1 and 5")
    private Integer overallRating;

    @Min(value = 1, message = "Cleanliness rating must be between 1 and 5")
    @Max(value = 5, message = "Cleanliness rating must be between 1 and 5")
    private Integer cleanlinessRating;

    @Min(value = 1, message = "Comfort rating must be between 1 and 5")
    @Max(value = 5, message = "Comfort rating must be between 1 and 5")
    private Integer comfortRating;

    @Min(value = 1, message = "Service rating must be between 1 and 5")
    @Max(value = 5, message = "Service rating must be between 1 and 5")
    private Integer serviceRating;

    @Min(value = 1, message = "Value for money rating must be between 1 and 5")
    @Max(value = 5, message = "Value for money rating must be between 1 and 5")
    private Integer valueForMoneyRating;

    @Min(value = 1, message = "Location rating must be between 1 and 5")
    @Max(value = 5, message = "Location rating must be between 1 and 5")
    private Integer locationRating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
