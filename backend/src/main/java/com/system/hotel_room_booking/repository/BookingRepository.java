package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.Booking;
import com.system.hotel_room_booking.model.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    Optional<Booking> findByConfirmationNumber(String confirmationNumber);

    List<Booking> findByUserId(Long userId);
    
    List<Booking> findByRoomIdAndStatus(Long roomId, BookingStatus status);    List<Booking> findByRoomId(Long roomId);
    
    List<Booking> findByStatus(BookingStatus status);
    
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Booking b WHERE b.checkInDate <= :date AND b.checkOutDate >= :date")
    List<Booking> findByDate(@Param("date") LocalDate date);
    
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn " +
           "AND b.status NOT IN ('CANCELLED', 'NO_SHOW')")
    List<Booking> findConflictingBookings(@Param("roomId") Long roomId,
                                          @Param("checkIn") LocalDate checkIn,
                                          @Param("checkOut") LocalDate checkOut);
    
    @Query("SELECT b FROM Booking b WHERE b.checkInDate = :tomorrow AND b.status = 'CONFIRMED'")
    List<Booking> findUpcomingCheckIns(@Param("tomorrow") LocalDate tomorrow);
    
    @Query("SELECT b FROM Booking b WHERE b.checkOutDate = :tomorrow AND b.status = 'CHECKED_IN'")
    List<Booking> findUpcomingCheckOuts(@Param("tomorrow") LocalDate tomorrow);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    Long countByStatus(@Param("status") BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.createdAt BETWEEN :start AND :end")
    List<Booking> findBookingsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
