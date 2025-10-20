package com.system.hotel_room_booking.model.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for PayWay callback parameters
 * Received when PayWay calls the return URL after payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbaPayWayCallbackRequest {
    
    private String tranId;            // Transaction ID
    private String reqTime;           // Request time
    private Integer status;           // Payment status (0: success, 1: pending, 2: failed, 3: cancelled)
    private String hash;              // Hash for verification
    private String paymentOption;     // Payment method used
    private String returnParams;      // Custom params sent in original request
    private String message;           // Status message
}
