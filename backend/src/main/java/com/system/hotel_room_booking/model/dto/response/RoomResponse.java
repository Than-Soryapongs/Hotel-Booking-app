package com.system.hotel_room_booking.model.dto.response;

import com.system.hotel_room_booking.model.entity.RoomStatus;
import com.system.hotel_room_booking.model.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long id;
    private String roomNumber;
    private RoomType type;
    private RoomStatus status;
    private BigDecimal basePrice;
    private Integer capacity;
    private Integer bedCount;
    private Double size;
    private String floor;
    private String description;
    private Boolean isActive;
    private List<RoomImageResponse> images;
    private Set<AmenityResponse> amenities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
