package com.system.hotel_room_booking.model.dto.request;

import com.system.hotel_room_booking.model.entity.DiscountType;
import jakarta.validation.constraints.*;
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
public class CreateDiscountRequest {

    @NotBlank(message = "Discount code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must contain only uppercase letters, numbers, hyphens, and underscores")
    private String code;

    @NotBlank(message = "Discount name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType type;

    @DecimalMin(value = "0.0", message = "Percentage value must be positive")
    @DecimalMax(value = "100.0", message = "Percentage value must not exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Invalid percentage format")
    private BigDecimal percentageValue;

    @DecimalMin(value = "0.0", message = "Fixed amount must be positive")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal fixedAmount;

    @NotNull(message = "Valid from date is required")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid until date is required")
    private LocalDateTime validUntil;

    @Min(value = 1, message = "Max usage count must be at least 1")
    private Integer maxUsageCount;

    @Min(value = 1, message = "Max usage per user must be at least 1")
    private Integer maxUsagePerUser;

    @DecimalMin(value = "0.0", message = "Min order amount must be positive")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", message = "Max discount amount must be positive")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal maxDiscountAmount;

    @Size(max = 1000, message = "Terms and conditions must not exceed 1000 characters")
    private String termsAndConditions;

    @Builder.Default
    private Boolean isActive = true;
}
