package com.system.hotel_booking.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilePictureResponse {
    private String profileImageUrl;
    private String message;
    private Long fileSize;
    private String fileName;
}

