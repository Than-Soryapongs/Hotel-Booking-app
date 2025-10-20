package com.system.hotel_room_booking.model.dto.response;

import com.system.hotel_room_booking.model.entity.CartStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long id;
    private CartStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private String discountCode;
    private BigDecimal totalPrice;
    private Integer itemCount;
    private List<CartItemResponse> items;
}
