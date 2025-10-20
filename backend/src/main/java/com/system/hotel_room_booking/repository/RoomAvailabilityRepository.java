package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.RoomAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, Long> {
    
    Optional<RoomAvailability> findByRoomIdAndDate(Long roomId, LocalDate date);
    
    List<RoomAvailability> findByRoomId(Long roomId);
    
    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.room.id = :roomId " +
           "AND ra.date BETWEEN :startDate AND :endDate")
    List<RoomAvailability> findByRoomIdAndDateRange(@Param("roomId") Long roomId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.date BETWEEN :startDate AND :endDate " +
           "AND ra.isAvailable = false")
    List<RoomAvailability> findUnavailableDatesBetween(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
}
