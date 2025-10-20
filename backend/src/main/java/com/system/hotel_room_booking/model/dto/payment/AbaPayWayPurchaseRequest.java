package com.system.hotel_room_booking.model.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for ABA PayWay Purchase API
 * Based on official PayWay API documentation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbaPayWayPurchaseRequest {

    // Required fields
    private String reqTime;           // Request date and time in UTC format as YYYYMMDDHHmmss
    private String merchantId;        // Unique merchant key (max 30 chars)
    private String tranId;            // Unique transaction identifier (max 20 chars)
    private BigDecimal amount;        // Purchase amount
    private String hash;              // Base64 encoded HMAC SHA512 hash

    // Optional customer fields
    private String firstname;         // Buyer's first name (max 20 chars)
    private String lastname;          // Buyer's last name (max 20 chars)
    private String email;             // Buyer's email (max 50 chars)
    private String phone;             // Buyer's phone (max 20 chars)

    // Optional callback URLs
    private String returnUrl;         // URL for payment notification upon success
    private String cancelUrl;         // URL to redirect after user cancels payment
    private String continueSuccessUrl; // URL to redirect after successful payment

    // Optional configuration
    private Integer skipSuccessPage;   // 0: Don't skip, 1: Skip success page
    private String returnParams;       // Information to include in return URL callback
    private String viewType;           // "hosted_view" or "popup"
    private Integer paymentGate;       // 0 for Checkout service
    private Integer lifetime;          // Payment lifetime in minutes (min: 3, max: 30 days)

    // Optional transaction details
    private String items;              // Item details
    private String shipping;           // Shipping details
    private String currency;           // Currency code (default: USD)
    private String type;               // Payment type
    private String paymentOption;      // Payment method option
    private String customFields;       // Custom fields for merchant use
    private String returnDeeplink;     // Deep link for mobile apps
    private String payout;             // Payout information
    private String additionalParams;   // Additional parameters
    private String googlePayToken;     // Google Pay token if applicable
}
