package com.system.hotel_room_booking.model.dto.response;

import com.system.hotel_room_booking.model.entity.DiscountType;
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
public class DiscountResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private DiscountType type;
    private BigDecimal percentageValue;
    private BigDecimal fixedAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer maxUsageCount;
    private Integer currentUsageCount;
    private Integer maxUsagePerUser;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Boolean isActive;
    private String termsAndConditions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
