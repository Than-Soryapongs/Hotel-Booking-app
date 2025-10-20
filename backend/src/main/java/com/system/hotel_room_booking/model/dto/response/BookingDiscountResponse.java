package com.system.hotel_room_booking.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDiscountResponse {

    private Long id;
    private String discountCode;
    private BigDecimal discountAmount;
    private LocalDateTime appliedAt;
}
