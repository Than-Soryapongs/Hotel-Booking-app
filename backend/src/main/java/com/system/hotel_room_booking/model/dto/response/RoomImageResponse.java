package com.system.hotel_room_booking.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomImageResponse {

    private Long id;
    private String imageUrl;
    private String caption;
    private Integer displayOrder;
    private Boolean isPrimary;
}
