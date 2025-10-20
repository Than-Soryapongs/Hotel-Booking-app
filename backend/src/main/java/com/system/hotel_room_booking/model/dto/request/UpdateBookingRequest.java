package com.system.hotel_room_booking.model.dto.request;

import com.system.hotel_room_booking.model.entity.BookingStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingRequest {

    private BookingStatus status;

    @FutureOrPresent(message = "Check-in date must be today or in the future")
    private LocalDate checkInDate;

    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "Number of guests must be at least 1")
    @Max(value = 20, message = "Number of guests must not exceed 20")
    private Integer numberOfGuests;

    @Size(max = 1000, message = "Special requests must not exceed 1000 characters")
    private String specialRequests;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String cancellationReason;
}
