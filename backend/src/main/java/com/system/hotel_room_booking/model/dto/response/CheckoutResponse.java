package com.system.hotel_room_booking.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for checkout initiation
 * Contains payment URL and all form fields needed for PayWay submission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {
    
    // Transaction details
    private String transactionId;      // Internal transaction ID (tran_id)
    private String paymentUrl;         // PayWay Purchase API URL for form submission
    private BigDecimal totalAmount;    // Total amount to pay
    private String currency;           // Currency code
    private String message;            // Response message
    private Boolean success;           // Operation success status
    
    // PayWay form fields
    private String merchantId;         // PayWay merchant ID
    private String reqTime;            // Request timestamp
    private String hash;               // HMAC SHA512 hash for security
    
    // Customer details
    private String firstName;          // Customer first name
    private String lastName;           // Customer last name
    private String email;              // Customer email
    
    // Callback URLs
    private String returnUrl;          // Return URL after payment
    private String cancelUrl;          // Cancel URL if payment cancelled
    private String continueSuccessUrl; // Continue success URL
}

