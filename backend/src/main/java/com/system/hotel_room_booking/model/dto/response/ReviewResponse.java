package com.system.hotel_room_booking.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private UserSummaryResponse user;
    private RoomSummaryResponse room;
    private Integer overallRating;
    private Integer cleanlinessRating;
    private Integer comfortRating;
    private Integer serviceRating;
    private Integer valueForMoneyRating;
    private Integer locationRating;
    private String comment;
    private Boolean isVerified;
    private Boolean isPublished;
    private LocalDateTime publishedAt;
    private String managementResponse;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
}
