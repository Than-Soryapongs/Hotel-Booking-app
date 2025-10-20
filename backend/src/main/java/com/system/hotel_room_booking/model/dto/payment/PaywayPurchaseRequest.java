package com.system.hotel_room_booking.model.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaywayPurchaseRequest {
    @NotBlank
    @Size(max = 20)
    private String tranId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @Size(max = 20)
    private String firstname;

    @Size(max = 20)
    private String lastname;

    @Size(max = 50)
    private String email;

    @Size(max = 20)
    private String phone;

    // Optional overrides – defaulted from properties if not provided
    private String returnUrl;
    private String cancelUrl;
    private String continueSuccessUrl;
    private Integer skipSuccessPage; // 0 or 1
}
