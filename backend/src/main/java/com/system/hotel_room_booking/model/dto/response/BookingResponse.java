package com.system.hotel_room_booking.model.dto.response;

import com.system.hotel_room_booking.model.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String confirmationNumber;
    private UserSummaryResponse user;
    private RoomSummaryResponse room;
    private BookingStatus status;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfGuests;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String specialRequests;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private List<BookingDiscountResponse> appliedDiscounts;
    private ReviewResponse review;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
