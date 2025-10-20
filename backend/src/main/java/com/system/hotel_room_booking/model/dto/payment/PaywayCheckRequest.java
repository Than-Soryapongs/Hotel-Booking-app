package com.system.hotel_room_booking.model.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaywayCheckRequest {
    @NotBlank
    private String tranId;
}
