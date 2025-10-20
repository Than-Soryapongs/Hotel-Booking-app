package com.system.hotel_room_booking.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutInitResponse {
    private String tranId;
    private String redirectUrl;
}
