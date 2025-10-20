package com.system.hotel_room_booking.model.dto.request;

import com.system.hotel_room_booking.model.entity.RoomStatus;
import com.system.hotel_room_booking.model.entity.RoomType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(max = 10, message = "Room number must not exceed 10 characters")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType type;

    @NotNull(message = "Room status is required")
    private RoomStatus status;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    private BigDecimal basePrice;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity must not exceed 20")
    private Integer capacity;

    @Min(value = 1, message = "Bed count must be at least 1")
    @Max(value = 10, message = "Bed count must not exceed 10")
    private Integer bedCount;

    @DecimalMin(value = "0.0", message = "Size must be positive")
    private Double size;

    @NotBlank(message = "Floor is required")
    @Size(max = 10, message = "Floor must not exceed 10 characters")
    private String floor;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Set<Long> amenityIds;

    @Builder.Default
    private Boolean isActive = true;
}
