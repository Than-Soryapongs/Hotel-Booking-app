package com.system.hotel_room_booking.repository;

import com.system.hotel_room_booking.model.entity.Room;
import com.system.hotel_room_booking.model.entity.RoomStatus;
import com.system.hotel_room_booking.model.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    Optional<Room> findByRoomNumber(String roomNumber);
    
    List<Room> findByType(RoomType type);
    
    List<Room> findByStatus(RoomStatus status);
    
    List<Room> findByIsActiveTrue();
    
    List<Room> findByTypeAndStatus(RoomType type, RoomStatus status);
    
    @Query("SELECT r FROM Room r WHERE r.basePrice BETWEEN :minPrice AND :maxPrice")
    List<Room> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity")
    List<Room> findByMinimumCapacity(@Param("minCapacity") Integer minCapacity);
    
    @Query("SELECT r FROM Room r WHERE r.id NOT IN " +
           "(SELECT b.room.id FROM Booking b WHERE b.checkInDate <= :checkOut " +
           "AND b.checkOutDate >= :checkIn " +
           "AND b.status NOT IN ('CANCELLED', 'NO_SHOW'))")
    List<Room> findAvailableRooms(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);
    
    @Query("SELECT r FROM Room r WHERE r.type = :type AND r.id NOT IN " +
           "(SELECT b.room.id FROM Booking b WHERE b.checkInDate <= :checkOut " +
           "AND b.checkOutDate >= :checkIn " +
           "AND b.status NOT IN ('CANCELLED', 'NO_SHOW'))")
    List<Room> findAvailableRoomsByType(@Param("type") RoomType type, 
                                        @Param("checkIn") LocalDate checkIn, 
                                        @Param("checkOut") LocalDate checkOut);
    
    @Query("SELECT COUNT(r) FROM Room r WHERE r.status = :status")
    Long countByStatus(@Param("status") RoomStatus status);
}
