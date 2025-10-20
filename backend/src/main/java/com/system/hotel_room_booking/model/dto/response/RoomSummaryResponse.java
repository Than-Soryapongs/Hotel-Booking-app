package com.system.hotel_room_booking.model.dto.response;

import com.system.hotel_room_booking.model.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSummaryResponse {

    private Long id;
    private String roomNumber;
    private RoomType type;
    private String floor;
    private BigDecimal basePrice;
}
