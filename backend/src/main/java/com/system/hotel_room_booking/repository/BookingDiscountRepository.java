package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.BookingDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDiscountRepository extends JpaRepository<BookingDiscount, Long> {
    
    List<BookingDiscount> findByBookingId(Long bookingId);
    
    List<BookingDiscount> findByDiscountId(Long discountId);
}
