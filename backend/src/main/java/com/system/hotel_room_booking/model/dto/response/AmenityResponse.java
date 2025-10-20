package com.system.hotel_room_booking.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityResponse {

    private Long id;
    private String name;
    private String category;
    private String icon;
    private String description;
    private Boolean isActive;
}
