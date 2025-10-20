package com.system.hotel_room_booking.model.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO from ABA PayWay Purchase API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbaPayWayPurchaseResponse {
    
    private Integer status;           // Response status code
    private String message;           // Response message
    private String tranId;            // Transaction ID
    private String paymentUrl;        // URL to redirect customer for payment
    private String qrCode;            // QR code data (if applicable)
    private BigDecimal amount;        // Transaction amount
    private String currency;          // Currency code
}
